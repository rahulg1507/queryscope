package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.parser.Lexer;
import com.queryscope.backend.engine.parser.Parser;
import com.queryscope.backend.engine.plan.JoinStrategy;
import com.queryscope.backend.engine.storage.ColumnDefinition;
import com.queryscope.backend.engine.storage.DataType;
import com.queryscope.backend.engine.storage.Database;
import com.queryscope.backend.engine.storage.Table;
import com.queryscope.backend.engine.storage.TableSchema;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HashJoinOperatorTest {

    @Test
    void supportsStringKeysAndDuplicateMatches() {
        Database database = database(DataType.STRING);
        database.getTable("left").insert(Map.of("key", "a", "value", "left-a"));
        database.getTable("left").insert(Map.of("key", "b", "value", "left-b"));
        database.getTable("right").insert(Map.of("key", "a", "value", "right-a1"));
        database.getTable("right").insert(Map.of("key", "a", "value", "right-a2"));

        QueryResult result = execute(database, "SELECT left.key, right.value FROM left JOIN right ON left.key = right.key");

        assertThat(result.columns()).extracting(ResultColumn::name).containsExactly("left.key", "right.value");
        assertThat(result.rows()).containsExactly(
                List.of("a", "right-a1"), List.of("a", "right-a2")
        );
        assertThat(join(result).details()).containsEntry("matches", 2);
    }

    @Test
    void supportsBooleanKeysAndEmptyInputs() {
        Database database = database(DataType.BOOLEAN);
        database.getTable("right").insert(Map.of("key", true, "value", "right-true"));
        database.getTable("right").insert(Map.of("key", true, "value", "right-true-2"));

        QueryResult emptyLeft = execute(database, "SELECT left.key, right.value FROM left JOIN right ON left.key = right.key");
        assertThat(emptyLeft.rows()).isEmpty();
        assertThat(join(emptyLeft).details()).containsEntry("buildRows", 0).containsEntry("matches", 0);

        database = database(DataType.BOOLEAN);
        database.getTable("left").insert(Map.of("key", false, "value", "left-false"));
        QueryResult emptyRight = execute(database, "SELECT left.key, right.value FROM left JOIN right ON left.key = right.key");
        assertThat(emptyRight.rows()).isEmpty();
        assertThat(join(emptyRight).details()).containsEntry("buildRows", 0).containsEntry("matches", 0);
    }

    @Test
    void rejectsMismatchedKeyTypes() {
        Database database = database(DataType.INTEGER);
        TableSchema stringSchema = new TableSchema(List.of(
                new ColumnDefinition("key", DataType.STRING),
                new ColumnDefinition("value", DataType.STRING)
        ));
        database.createTable(new Table("strings", stringSchema));

        assertThatThrownBy(() -> execute(database, "SELECT left.key, strings.value FROM left JOIN strings ON left.key = strings.key"))
                .isInstanceOf(QueryExecutionException.class)
                .hasMessage("Type mismatch in JOIN condition: cannot compare INTEGER with STRING.");
    }

    private static Database database(DataType keyType) {
        Database database = new Database();
        TableSchema schema = new TableSchema(List.of(
                new ColumnDefinition("key", keyType),
                new ColumnDefinition("value", DataType.STRING)
        ));
        database.createTable(new Table("left", schema));
        database.createTable(new Table("right", schema));
        return database;
    }

    private static QueryResult execute(Database database, String sql) {
        return new InMemoryQueryExecutor(database).execute(
                new Parser(new Lexer(sql).tokenize()).parse(), JoinStrategy.HASH
        );
    }

    private static com.queryscope.backend.engine.plan.ExecutionPlanNodeDto join(QueryResult result) {
        return result.executionPlan().children().isEmpty() ? result.executionPlan() : result.executionPlan().children().get(0);
    }
}
