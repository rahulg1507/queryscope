package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.ast.SelectStatement;
import com.queryscope.backend.engine.index.TableIndex;
import com.queryscope.backend.engine.parser.Lexer;
import com.queryscope.backend.engine.parser.Parser;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndexExecutionTest {

    private Database database;
    private QueryExecutor executor;

    @BeforeEach
    void setUp() {
        database = new Database();
        Table expenses = new Table("expenses", new TableSchema(List.of(
                new ColumnDefinition("id", DataType.INTEGER),
                new ColumnDefinition("description", DataType.STRING),
                new ColumnDefinition("amount", DataType.INTEGER)
        )));
        expenses.insert(Map.of("id", 1, "description", "Dinner", "amount", 90));
        expenses.insert(Map.of("id", 2, "description", "Hotel", "amount", 300));
        expenses.insert(Map.of("id", 3, "description", "Taxi", "amount", 40));
        expenses.insert(Map.of("id", 4, "description", "Movie", "amount", 60));
        database.createTable(expenses);
        executor = new InMemoryQueryExecutor(database);
    }

    @Test
    void createsIndexFromExistingRowsAndIndexesLaterInserts() {
        TableIndex index = database.createIndex("idx_amount", "expenses", "amount");
        assertThat(index.entryCount()).isEqualTo(4);
        database.requireTable("expenses").insert(Map.of("id", 5, "description", "Coffee", "amount", 90));
        assertThat(index.entryCount()).isEqualTo(5);
        assertThat(index.distinctKeyCount()).isEqualTo(4);
        index.validateInvariants();
    }

    @Test
    void executesExactAndRangeIndexScansWithMeasuredPlanDetails() {
        database.createIndex("idx_amount", "expenses", "amount");
        QueryResult tableResult = execute("SELECT description, amount FROM expenses WHERE amount = 300", ScanStrategy.TABLE);
        QueryResult indexResult = execute("SELECT description, amount FROM expenses WHERE amount = 300", ScanStrategy.INDEX);

        assertThat(tableResult.rows()).containsExactly(List.of("Hotel", 300L));
        assertThat(indexResult.rows()).containsExactly(List.of("Hotel", 300L));
        assertThat(tableResult.executionPlan().children().get(0).type()).isEqualTo("FILTER");
        var indexPlan = indexResult.executionPlan().children().get(0);
        assertThat(indexPlan.type()).isEqualTo("INDEX_SCAN");
        assertThat(indexPlan.details()).containsEntry("index", "idx_amount")
                .containsEntry("column", "amount")
                .containsEntry("indexLookups", 1)
                .containsEntry("rowsReturned", 1);

        QueryResult range = execute("SELECT description, amount FROM expenses WHERE amount > 50", ScanStrategy.INDEX);
        assertThat(range.rows()).containsExactlyInAnyOrder(
                List.of("Dinner", 90L), List.of("Hotel", 300L), List.of("Movie", 60L));
        assertThat(range.executionPlan().children().get(0).type()).isEqualTo("INDEX_SCAN");
    }

    @Test
    void rejectsMissingWrongAndUnsupportedIndexUse() {
        assertThatThrownBy(() -> execute("SELECT description FROM expenses WHERE amount = 300", ScanStrategy.INDEX))
                .isInstanceOf(QueryExecutionException.class)
                .hasMessage("No usable index exists for predicate 'amount = 300'.");
        database.createIndex("idx_description", "expenses", "description");
        assertThatThrownBy(() -> execute("SELECT description FROM expenses WHERE amount = 300", ScanStrategy.INDEX))
                .isInstanceOf(QueryExecutionException.class)
                .hasMessage("No usable index exists for predicate 'amount = 300'.");
        database.createIndex("idx_amount", "expenses", "amount");
        assertThatThrownBy(() -> database.createIndex("idx_amount", "expenses", "amount"))
                .isInstanceOf(QueryExecutionException.class)
                .hasMessage("Index 'idx_amount' already exists.");
    }

    @Test
    void keepsDefaultTableScanAndRejectsIndexWithoutPredicate() {
        QueryResult result = execute("SELECT * FROM expenses", ScanStrategy.TABLE);
        assertThat(result.executionPlan().type()).isEqualTo("TABLE_SCAN");
        assertThatThrownBy(() -> execute("SELECT * FROM expenses", ScanStrategy.INDEX))
                .isInstanceOf(QueryExecutionException.class)
                .hasMessage("Index scan requires a WHERE equality or range predicate.");
    }

    private QueryResult execute(String sql, ScanStrategy scanStrategy) {
        SelectStatement statement = new Parser(new Lexer(sql).tokenize()).parse();
        return executor.execute(statement, com.queryscope.backend.engine.plan.JoinStrategy.NESTED_LOOP, scanStrategy);
    }
}
