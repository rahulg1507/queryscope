package com.queryscope.backend.engine.optimizer;

import com.queryscope.backend.engine.ast.SelectStatement;
import com.queryscope.backend.engine.execution.InMemoryQueryExecutor;
import com.queryscope.backend.engine.execution.QueryResult;
import com.queryscope.backend.engine.parser.Lexer;
import com.queryscope.backend.engine.parser.Parser;
import com.queryscope.backend.engine.plan.ExecutionMode;
import com.queryscope.backend.engine.plan.JoinStrategy;
import com.queryscope.backend.engine.plan.ScanStrategy;
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

class QueryOptimizerTest {

    private Database database;
    private InMemoryQueryExecutor executor;

    @BeforeEach
    void setUp() {
        database = new Database();
        Table users = new Table("users", new TableSchema(List.of(
                new ColumnDefinition("id", DataType.INTEGER),
                new ColumnDefinition("name", DataType.STRING),
                new ColumnDefinition("age", DataType.INTEGER)
        )));
        users.insert(Map.of("id", 1, "name", "Rahul", "age", 19));
        users.insert(Map.of("id", 2, "name", "Aayan", "age", 21));
        Table expenses = new Table("expenses", new TableSchema(List.of(
                new ColumnDefinition("id", DataType.INTEGER),
                new ColumnDefinition("user_id", DataType.INTEGER),
                new ColumnDefinition("amount", DataType.INTEGER)
        )));
        expenses.insert(Map.of("id", 10, "user_id", 1, "amount", 90));
        expenses.insert(Map.of("id", 11, "user_id", 2, "amount", 300));
        database.createTable(users);
        database.createTable(expenses);
        executor = new InMemoryQueryExecutor(database);
    }

    @Test
    void choosesMatchingEqualityAndRangeIndexesWithStructuredTrace() {
        database.createIndex("idx_amount", "expenses", "amount");

        QueryResult exact = execute("SELECT amount FROM expenses WHERE amount = 300", ExecutionMode.AUTO,
                JoinStrategy.NESTED_LOOP, ScanStrategy.TABLE);
        QueryResult range = execute("SELECT amount FROM expenses WHERE amount > 50", ExecutionMode.AUTO,
                JoinStrategy.NESTED_LOOP, ScanStrategy.TABLE);

        assertThat(exact.executionPlan().children().get(0).type()).isEqualTo("INDEX_SCAN");
        assertThat(range.executionPlan().children().get(0).type()).isEqualTo("INDEX_SCAN");
        assertThat(exact.optimization().mode()).isEqualTo("AUTO");
        assertThat(exact.optimization().rulesApplied()).anyMatch(trace -> trace.rule().equals("MATCHING_INDEX"));
        assertThat(exact.optimization().rulesApplied().get(0).decision()).isEqualTo("USE_INDEX_SCAN");
    }

    @Test
    void fallsBackToTableScanForMissingOrWrongColumnIndex() {
        database.createIndex("idx_amount", "expenses", "amount");

        QueryResult result = execute("SELECT name FROM users WHERE age > 18", ExecutionMode.AUTO,
                JoinStrategy.NESTED_LOOP, ScanStrategy.INDEX);

        assertThat(result.executionPlan().children().get(0).type()).isEqualTo("FILTER");
        assertThat(result.optimization().rulesApplied()).anyMatch(trace -> trace.rule().equals("NO_MATCHING_INDEX"));
    }

    @Test
    void choosesHashJoinAutomaticallyAndManualModePreservesRequestedStrategies() {
        String sql = "SELECT users.name, expenses.amount FROM users JOIN expenses ON users.id = expenses.user_id";
        QueryResult auto = execute(sql, ExecutionMode.AUTO, JoinStrategy.NESTED_LOOP, ScanStrategy.TABLE);
        QueryResult manualNested = execute(sql, ExecutionMode.MANUAL, JoinStrategy.NESTED_LOOP, ScanStrategy.TABLE);
        QueryResult manualHash = execute(sql, ExecutionMode.MANUAL, JoinStrategy.HASH, ScanStrategy.TABLE);

        assertThat(auto.executionPlan().children().get(0).type()).isEqualTo("HASH_JOIN");
        assertThat(auto.optimization().rulesApplied()).anyMatch(trace -> trace.rule().equals("EQUALITY_JOIN"));
        assertThat(manualNested.executionPlan().children().get(0).type()).isEqualTo("NESTED_LOOP_JOIN");
        assertThat(manualHash.executionPlan().children().get(0).type()).isEqualTo("HASH_JOIN");
        assertThat(auto.rows()).containsExactlyInAnyOrderElementsOf(manualNested.rows());
        assertThat(auto.rows()).containsExactlyInAnyOrderElementsOf(manualHash.rows());
        assertThat(manualNested.optimization().rulesApplied().get(0).rule()).isEqualTo("MANUAL_STRATEGIES");
    }

    @Test
    void fallsBackToNestedLoopForUnsupportedHashKeyType() {
        Table left = new Table("double_left", new TableSchema(List.of(
                new ColumnDefinition("key", DataType.DOUBLE), new ColumnDefinition("value", DataType.STRING))));
        Table right = new Table("double_right", new TableSchema(List.of(
                new ColumnDefinition("key", DataType.DOUBLE), new ColumnDefinition("value", DataType.STRING))));
        left.insert(Map.of("key", 1.5, "value", "left"));
        right.insert(Map.of("key", 1.5, "value", "right"));
        database.createTable(left);
        database.createTable(right);

        QueryResult result = execute(
                "SELECT double_left.value, double_right.value FROM double_left JOIN double_right ON double_left.key = double_right.key",
                ExecutionMode.AUTO, JoinStrategy.HASH, ScanStrategy.TABLE);

        assertThat(result.executionPlan().children().get(0).type()).isEqualTo("NESTED_LOOP_JOIN");
        assertThat(result.optimization().rulesApplied()).anyMatch(trace -> trace.rule().equals("HASH_JOIN_FALLBACK"));
    }

    private QueryResult execute(String sql, ExecutionMode mode, JoinStrategy joinStrategy, ScanStrategy scanStrategy) {
        SelectStatement statement = new Parser(new Lexer(sql).tokenize()).parse();
        return executor.execute(statement, mode, joinStrategy, scanStrategy);
    }
}
