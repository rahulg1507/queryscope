package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.ast.SelectStatement;
import com.queryscope.backend.engine.parser.Lexer;
import com.queryscope.backend.engine.parser.Parser;
import com.queryscope.backend.engine.storage.ColumnDefinition;
import com.queryscope.backend.engine.storage.DataType;
import com.queryscope.backend.engine.storage.Database;
import com.queryscope.backend.engine.storage.Table;
import com.queryscope.backend.engine.storage.TableSchema;
import com.queryscope.backend.engine.plan.JoinStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryQueryExecutorTest {

    private Database database;
    private QueryExecutor executor;

    @BeforeEach
    void setUp() {
        database = new Database();
        TableSchema schema = new TableSchema(List.of(
                new ColumnDefinition("id", DataType.INTEGER),
                new ColumnDefinition("name", DataType.STRING),
                new ColumnDefinition("age", DataType.INTEGER),
                new ColumnDefinition("active", DataType.BOOLEAN)
        ));
        Table users = new Table("users", schema);
        users.insert(Map.of("id", 1, "name", "Rahul", "age", 19, "active", true));
        users.insert(Map.of("id", 2, "name", "Aayan", "age", 21, "active", true));
        users.insert(Map.of("id", 3, "name", "John", "age", 17, "active", false));
        users.insert(Map.of("id", 4, "name", "Maya", "age", 25, "active", true));
        database.createTable(users);

        TableSchema expensesSchema = new TableSchema(List.of(
                new ColumnDefinition("id", DataType.INTEGER),
                new ColumnDefinition("user_id", DataType.INTEGER),
                new ColumnDefinition("description", DataType.STRING),
                new ColumnDefinition("amount", DataType.INTEGER)
        ));
        Table expenses = new Table("expenses", expensesSchema);
        expenses.insert(Map.of("id", 1, "user_id", 1, "description", "Dinner", "amount", 90));
        expenses.insert(Map.of("id", 2, "user_id", 2, "description", "Hotel", "amount", 300));
        expenses.insert(Map.of("id", 3, "user_id", 1, "description", "Taxi", "amount", 40));
        expenses.insert(Map.of("id", 4, "user_id", 3, "description", "Movie", "amount", 60));
        database.createTable(expenses);
        executor = new InMemoryQueryExecutor(database);
    }

    @Test
    void returnsAllRowsAndColumnsForWildcard() {
        QueryResult result = execute("SELECT * FROM users;");

        assertThat(result.columns()).extracting(ResultColumn::name)
                .containsExactly("id", "name", "age", "active");
        assertThat(result.rows()).hasSize(4);
        assertThat(result.rows().get(0)).containsExactly(1L, "Rahul", 19L, true);
        assertThat(result.metrics()).isEqualTo(new ExecutionMetrics(4, 4));
        assertThat(result.executionPlan().type()).isEqualTo("TABLE_SCAN");
        assertThat(result.executionPlan().inputRows()).isEqualTo(4);
        assertThat(result.executionPlan().outputRows()).isEqualTo(4);
        assertThat(result.executionPlan().children()).isEmpty();
    }

    @Test
    void projectsOneAndMultipleColumnsInSelectOrder() {
        QueryResult oneColumn = execute("SELECT name FROM users");
        assertThat(oneColumn.columns()).extracting(ResultColumn::name)
                .containsExactly("name");
        assertThat(oneColumn.executionPlan().type()).isEqualTo("PROJECTION");
        assertThat(oneColumn.executionPlan().children()).singleElement()
                .extracting(node -> node.type()).isEqualTo("TABLE_SCAN");
        assertThat(execute("SELECT name, age FROM users").rows())
                .containsExactly(List.of("Rahul", 19L), List.of("Aayan", 21L), List.of("John", 17L), List.of("Maya", 25L));
    }

    @Test
    void filtersIntegerStringAndBooleanComparisons() {
        assertThat(names(execute("SELECT name FROM users WHERE age > 18"))).containsExactly("Rahul", "Aayan", "Maya");
        assertThat(names(execute("SELECT name FROM users WHERE age >= 21"))).containsExactly("Aayan", "Maya");
        assertThat(names(execute("SELECT name FROM users WHERE age = 19"))).containsExactly("Rahul");
        assertThat(names(execute("SELECT name FROM users WHERE age != 19"))).containsExactly("Aayan", "John", "Maya");
        assertThat(names(execute("SELECT name FROM users WHERE age < 18"))).containsExactly("John");
        assertThat(names(execute("SELECT name FROM users WHERE age <= 19"))).containsExactly("Rahul", "John");
        assertThat(names(execute("SELECT name FROM users WHERE name = 'Rahul'"))).containsExactly("Rahul");
        assertThat(names(execute("SELECT name FROM users WHERE active = true"))).containsExactly("Rahul", "Aayan", "Maya");
    }

    @Test
    void exposesNestedPlanAndMeasuredMetadata() {
        QueryResult result = execute("SELECT name FROM users WHERE age > 18");

        assertThat(result.executionPlan().type()).isEqualTo("PROJECTION");
        assertThat(result.executionPlan().details()).containsEntry("columns", List.of("name"));
        assertThat(result.executionPlan().inputRows()).isEqualTo(3);
        assertThat(result.executionPlan().outputRows()).isEqualTo(3);

        var filter = result.executionPlan().children().get(0);
        assertThat(filter.type()).isEqualTo("FILTER");
        assertThat(filter.details()).containsEntry("condition", "age > 18");
        assertThat(filter.inputRows()).isEqualTo(4);
        assertThat(filter.outputRows()).isEqualTo(3);

        var scan = filter.children().get(0);
        assertThat(scan.type()).isEqualTo("TABLE_SCAN");
        assertThat(scan.details()).containsEntry("table", "users");
        assertThat(scan.inputRows()).isEqualTo(4);
        assertThat(scan.outputRows()).isEqualTo(4);
    }

    @Test
    void buildsFilterPlanForBooleanPredicate() {
        QueryResult result = execute("SELECT name FROM users WHERE active = true");

        assertThat(result.executionPlan().type()).isEqualTo("PROJECTION");
        assertThat(result.executionPlan().children().get(0).type()).isEqualTo("FILTER");
        assertThat(result.executionPlan().children().get(0).details())
                .containsEntry("condition", "active = true");
    }

    @Test
    void executesNestedLoopJoinAndExposesBranchingPlanMetadata() {
        QueryResult result = execute("SELECT users.name, expenses.amount FROM users JOIN expenses ON users.id = expenses.user_id");

        assertThat(result.columns()).extracting(ResultColumn::name)
                .containsExactly("users.name", "expenses.amount");
        assertThat(result.rows()).containsExactly(
                List.of("Rahul", 90L), List.of("Rahul", 40L),
                List.of("Aayan", 300L), List.of("John", 60L));
        assertThat(result.executionPlan().type()).isEqualTo("PROJECTION");
        var join = result.executionPlan().children().get(0);
        assertThat(join.type()).isEqualTo("NESTED_LOOP_JOIN");
        assertThat(join.details()).containsEntry("condition", "users.id = expenses.user_id")
                .containsEntry("leftRows", 4)
                .containsEntry("rightRows", 4)
                .containsEntry("comparisons", 16)
                .containsEntry("matches", 4);
        assertThat(join.inputRows()).isEqualTo(8);
        assertThat(join.outputRows()).isEqualTo(4);
        assertThat(join.children()).extracting(node -> node.type())
                .containsExactly("TABLE_SCAN", "TABLE_SCAN");
    }

    @Test
    void supportsWildcardJoinAndQualifiedDuplicateColumnNames() {
        QueryResult wildcard = execute("SELECT * FROM users JOIN expenses ON users.id = expenses.user_id");
        assertThat(wildcard.executionPlan().type()).isEqualTo("NESTED_LOOP_JOIN");
        assertThat(wildcard.columns()).extracting(ResultColumn::name)
                .containsExactly("users.id", "users.name", "users.age", "users.active",
                        "expenses.id", "expenses.user_id", "expenses.description", "expenses.amount");

        QueryResult qualifiedIds = execute("SELECT users.id, expenses.id FROM users JOIN expenses ON users.id = expenses.user_id");
        assertThat(qualifiedIds.columns()).extracting(ResultColumn::name)
                .containsExactly("users.id", "expenses.id");
    }

    @Test
    void executesHashJoinWithDeterministicBuildSideAndMetrics() {
        SelectStatement statement = new Parser(new Lexer(
                "SELECT users.name, expenses.amount FROM users JOIN expenses ON users.id = expenses.user_id"
        ).tokenize()).parse();
        QueryResult nestedLoop = executor.execute(statement, JoinStrategy.NESTED_LOOP);
        QueryResult hash = executor.execute(statement, JoinStrategy.HASH);

        assertThat(hash.executionPlan().type()).isEqualTo("PROJECTION");
        var join = hash.executionPlan().children().get(0);
        assertThat(join.type()).isEqualTo("HASH_JOIN");
        assertThat(join.details()).containsEntry("strategy", "HASH")
                .containsEntry("buildSide", "LEFT")
                .containsEntry("probeSide", "RIGHT")
                .containsEntry("buildRows", 4)
                .containsEntry("probeRows", 4)
                .containsEntry("rowsInserted", 4)
                .containsEntry("hashLookups", 4)
                .containsEntry("matches", 4);
        assertThat(hash.rows()).containsExactlyInAnyOrderElementsOf(nestedLoop.rows());
    }

    @Test
    void resolvesUniqueColumnsAndRejectsAmbiguousOrInvalidJoinReferences() {
        assertThat(execute("SELECT name FROM users JOIN expenses ON users.id = expenses.user_id").columns())
                .extracting(ResultColumn::name).containsExactly("name");
        assertThatThrownBy(() -> execute("SELECT id FROM users JOIN expenses ON users.id = expenses.user_id"))
                .isInstanceOf(QueryExecutionException.class)
                .hasMessage("Ambiguous column 'id'.");
        assertThatThrownBy(() -> execute("SELECT users.missing FROM users JOIN expenses ON users.id = expenses.user_id"))
                .isInstanceOf(QueryExecutionException.class)
                .hasMessage("Unknown column 'users.missing' in table 'users JOIN expenses'.");
        assertThatThrownBy(() -> execute("SELECT users.name FROM users JOIN expenses ON users.id = expenses.description"))
                .isInstanceOf(QueryExecutionException.class)
                .hasMessage("Type mismatch in JOIN condition: cannot compare INTEGER with STRING.");
    }

    @Test
    void appliesWhereAfterJoinAndKeepsJoinBranchingPlan() {
        QueryResult result = execute("SELECT users.name, expenses.amount FROM users JOIN expenses ON users.id = expenses.user_id WHERE expenses.amount > 100");

        assertThat(result.rows()).containsExactly(List.of("Aayan", 300L));
        assertThat(result.executionPlan().type()).isEqualTo("PROJECTION");
        assertThat(result.executionPlan().children().get(0).type()).isEqualTo("FILTER");
        assertThat(result.executionPlan().children().get(0).children().get(0).type()).isEqualTo("NESTED_LOOP_JOIN");
    }

    @Test
    void returnsEmptyResultWithColumnsAndMetricsWhenNothingMatches() {
        QueryResult result = execute("SELECT name, age FROM users WHERE age > 100");

        assertThat(result.columns()).extracting(ResultColumn::name).containsExactly("name", "age");
        assertThat(result.rows()).isEmpty();
        assertThat(result.rowCount()).isZero();
        assertThat(result.metrics()).isEqualTo(new ExecutionMetrics(4, 0));
    }

    @Test
    void rejectsUnknownTablesColumnsAndTypeMismatches() {
        assertThatThrownBy(() -> execute("SELECT name FROM missing"))
                .isInstanceOf(QueryExecutionException.class)
                .hasMessage("Unknown table 'missing'.");
        assertThatThrownBy(() -> execute("SELECT email FROM users"))
                .isInstanceOf(QueryExecutionException.class)
                .hasMessage("Unknown column 'email' in table 'users'.");
        assertThatThrownBy(() -> execute("SELECT name FROM users WHERE age > 'Rahul'"))
                .isInstanceOf(QueryExecutionException.class)
                .hasMessage("Type mismatch: cannot compare INTEGER with STRING.");
        assertThatThrownBy(() -> execute("SELECT name FROM users WHERE active > 5"))
                .isInstanceOf(QueryExecutionException.class)
                .hasMessage("Type mismatch: cannot compare BOOLEAN with INTEGER.");
    }

    private QueryResult execute(String sql) {
        SelectStatement statement = new Parser(new Lexer(sql).tokenize()).parse();
        return executor.execute(statement);
    }

    private static List<String> names(QueryResult result) {
        return result.rows().stream().map(row -> (String) row.get(0)).toList();
    }
}
