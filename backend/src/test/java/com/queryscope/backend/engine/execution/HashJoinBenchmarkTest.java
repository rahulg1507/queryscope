package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.ast.SelectStatement;
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

class HashJoinBenchmarkTest {

    @Test
    void comparesStrategiesOnAStableSyntheticFixture() {
        Database database = new Database();
        Table users = new Table("users", new TableSchema(List.of(
                new ColumnDefinition("id", DataType.INTEGER)
        )));
        for (int id = 1; id <= 100; id++) {
            users.insert(Map.of("id", id));
        }
        database.createTable(users);

        Table expenses = new Table("expenses", new TableSchema(List.of(
                new ColumnDefinition("id", DataType.INTEGER),
                new ColumnDefinition("user_id", DataType.INTEGER),
                new ColumnDefinition("amount", DataType.INTEGER)
        )));
        for (int id = 1; id <= 1_000; id++) {
            expenses.insert(Map.of("id", id, "user_id", ((id - 1) % 100) + 1, "amount", id));
        }
        database.createTable(expenses);

        SelectStatement statement = new Parser(new Lexer(
                "SELECT users.id, expenses.amount FROM users JOIN expenses ON users.id = expenses.user_id"
        ).tokenize()).parse();
        QueryExecutor executor = new InMemoryQueryExecutor(database);
        QueryResult nestedLoop = executor.execute(statement, JoinStrategy.NESTED_LOOP);
        QueryResult hash = executor.execute(statement, JoinStrategy.HASH);
        var nestedJoin = nestedLoop.executionPlan();
        var hashJoin = hash.executionPlan();

        assertThat(nestedJoin.type()).isEqualTo("PROJECTION");
        assertThat(nestedJoin.children().get(0).details()).containsEntry("comparisons", 100_000);
        assertThat(hashJoin.children().get(0).details()).containsEntry("buildRows", 100)
                .containsEntry("probeRows", 1_000)
                .containsEntry("hashLookups", 1_000)
                .containsEntry("rowsInserted", 100)
                .containsEntry("matches", 1_000);
        assertThat(hash.rows()).containsExactlyInAnyOrderElementsOf(nestedLoop.rows());
    }
}
