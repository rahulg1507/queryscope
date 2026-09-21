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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AggregationExecutionTest {

    private Database database;
    private QueryExecutor executor;

    @BeforeEach
    void setUp() {
        database = new Database();
        Table users = new Table("users", new TableSchema(List.of(
                new ColumnDefinition("id", DataType.INTEGER),
                new ColumnDefinition("name", DataType.STRING),
                new ColumnDefinition("age", DataType.INTEGER),
                new ColumnDefinition("active", DataType.BOOLEAN)
        )));
        users.insert(Map.of("id", 1, "name", "Rahul", "age", 19, "active", true));
        users.insert(Map.of("id", 2, "name", "Aayan", "age", 21, "active", true));
        users.insert(Map.of("id", 3, "name", "John", "age", 17, "active", false));
        users.insert(Map.of("id", 4, "name", "Maya", "age", 25, "active", true));
        database.createTable(users);

        Table expenses = new Table("expenses", new TableSchema(List.of(
                new ColumnDefinition("id", DataType.INTEGER),
                new ColumnDefinition("user_id", DataType.INTEGER),
                new ColumnDefinition("description", DataType.STRING),
                new ColumnDefinition("amount", DataType.INTEGER)
        )));
        expenses.insert(Map.of("id", 1, "user_id", 1, "description", "Dinner", "amount", 90));
        expenses.insert(Map.of("id", 2, "user_id", 2, "description", "Hotel", "amount", 300));
        expenses.insert(Map.of("id", 3, "user_id", 1, "description", "Taxi", "amount", 40));
        expenses.insert(Map.of("id", 4, "user_id", 3, "description", "Movie", "amount", 60));
        database.createTable(expenses);
        executor = new InMemoryQueryExecutor(database);
    }

    @Test
    void executesCountSumAndAvgWithoutGrouping() {
        assertThat(execute("SELECT COUNT(*) FROM users").rows()).containsExactly(List.of(4L));
        assertThat(execute("SELECT COUNT(age) FROM users").rows()).containsExactly(List.of(4L));
        assertThat(execute("SELECT SUM(amount) FROM expenses").rows()).containsExactly(List.of(490L));

        QueryResult average = execute("SELECT AVG(amount) FROM expenses");
        assertThat(average.rows()).containsExactly(List.of(122.5));
        assertThat(average.columns()).extracting(ResultColumn::type).containsExactly(DataType.DOUBLE);
    }

    @Test
    void groupsSumAvgAndCountInDeterministicInputOrder() {
        assertThat(execute("SELECT user_id, SUM(amount) FROM expenses GROUP BY user_id").rows())
                .containsExactly(List.of(1L, 130L), List.of(2L, 300L), List.of(3L, 60L));
        assertThat(execute("SELECT user_id, AVG(amount) FROM expenses GROUP BY user_id").rows())
                .containsExactly(List.of(1L, 65.0), List.of(2L, 300.0), List.of(3L, 60.0));
        assertThat(execute("SELECT user_id, COUNT(*) FROM expenses GROUP BY user_id").rows())
                .containsExactly(List.of(1L, 2L), List.of(2L, 1L), List.of(3L, 1L));
    }

    @Test
    void filtersBeforeGroupingAndExposesAggregatePlanMetrics() {
        QueryResult result = execute("SELECT user_id, SUM(amount) FROM expenses WHERE amount > 50 GROUP BY user_id");

        assertThat(result.rows()).containsExactly(List.of(1L, 90L), List.of(2L, 300L), List.of(3L, 60L));
        assertThat(result.executionPlan().type()).isEqualTo("PROJECTION");
        var aggregate = result.executionPlan().children().get(0);
        assertThat(aggregate.type()).isEqualTo("AGGREGATE");
        assertThat(aggregate.details()).containsEntry("groupBy", List.of("user_id"))
                .containsEntry("functions", List.of("SUM(amount)"))
                .containsEntry("groups", 3);
        assertThat(aggregate.inputRows()).isEqualTo(3);
        assertThat(aggregate.outputRows()).isEqualTo(3);
        assertThat(aggregate.children().get(0).type()).isEqualTo("FILTER");
    }

    @Test
    void aggregatesAfterNestedLoopAndHashJoins() {
        String sql = "SELECT users.name, SUM(expenses.amount) FROM users JOIN expenses "
                + "ON users.id = expenses.user_id GROUP BY users.name";
        QueryResult nested = executor.execute(parse(sql), JoinStrategy.NESTED_LOOP);
        QueryResult hash = executor.execute(parse(sql), JoinStrategy.HASH);

        assertThat(nested.rows()).containsExactly(List.of("Rahul", 130L), List.of("Aayan", 300L), List.of("John", 60L));
        assertThat(hash.rows()).containsExactlyElementsOf(nested.rows());
        assertThat(nested.executionPlan().children().get(0).children().get(0).type()).isEqualTo("NESTED_LOOP_JOIN");
        assertThat(hash.executionPlan().children().get(0).children().get(0).type()).isEqualTo("HASH_JOIN");
    }

    @Test
    void validatesGroupingAndAggregateTypes() {
        assertThatThrownBy(() -> execute("SELECT name, SUM(amount) FROM expenses GROUP BY user_id"))
                .isInstanceOf(QueryExecutionException.class)
                .hasMessage("Column 'name' must appear in GROUP BY or be aggregated.");
        assertThatThrownBy(() -> execute("SELECT SUM(name) FROM users"))
                .isInstanceOf(QueryExecutionException.class)
                .hasMessage("SUM requires a numeric column; 'name' is STRING.");
        assertThatThrownBy(() -> execute("SELECT AVG(active) FROM users"))
                .isInstanceOf(QueryExecutionException.class)
                .hasMessage("AVG requires a numeric column; 'active' is BOOLEAN.");
        assertThatThrownBy(() -> execute("SELECT COUNT(foo) FROM users"))
                .isInstanceOf(QueryExecutionException.class)
                .hasMessage("Unknown column 'foo' in table 'users'.");
    }

    @Test
    void countsEmptyInputAndRejectsEmptySumOrAverageClearly() {
        assertThat(execute("SELECT COUNT(*) FROM users WHERE age > 100").rows()).containsExactly(List.of(0L));
        assertThatThrownBy(() -> execute("SELECT SUM(amount) FROM expenses WHERE amount > 1000"))
                .isInstanceOf(QueryExecutionException.class)
                .hasMessage("SUM and AVG cannot produce a value for an empty input relation.");
        assertThat(execute("SELECT user_id, SUM(amount) FROM expenses WHERE amount > 1000 GROUP BY user_id").rows())
                .isEmpty();
    }

    private QueryResult execute(String sql) {
        return executor.execute(parse(sql));
    }

    private static SelectStatement parse(String sql) {
        return new Parser(new Lexer(sql).tokenize()).parse();
    }
}
