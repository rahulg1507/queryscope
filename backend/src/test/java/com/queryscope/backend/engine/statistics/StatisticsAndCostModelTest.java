package com.queryscope.backend.engine.statistics;

import com.queryscope.backend.engine.optimizer.CostModel;
import com.queryscope.backend.engine.plan.FilterCondition;
import com.queryscope.backend.engine.plan.PlanComparisonOperator;
import com.queryscope.backend.engine.storage.ColumnDefinition;
import com.queryscope.backend.engine.storage.DataType;
import com.queryscope.backend.engine.storage.Database;
import com.queryscope.backend.engine.storage.Table;
import com.queryscope.backend.engine.storage.TableSchema;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class StatisticsAndCostModelTest {

    @Test
    void collectsRowsDistinctValuesRangesAndIndexCounts() {
        Database database = new Database();
        Table accounts = new Table("accounts", new TableSchema(List.of(
                new ColumnDefinition("id", DataType.INTEGER),
                new ColumnDefinition("balance", DataType.DOUBLE),
                new ColumnDefinition("name", DataType.STRING)
        )));
        accounts.insert(Map.of("id", 1, "balance", 12.5, "name", "A"));
        accounts.insert(Map.of("id", 2, "balance", 30.0, "name", "B"));
        accounts.insert(Map.of("id", 2, "balance", 30.0, "name", "B"));
        database.createTable(accounts);
        database.createIndex("idx_id", "accounts", "id");

        TableStatistics statistics = new StatisticsManager(database).snapshot().table("ACCOUNTS");

        assertThat(statistics.rowCount()).isEqualTo(3);
        assertThat(statistics.column("id").distinctValues()).isEqualTo(2);
        assertThat(statistics.column("balance").min()).isEqualTo(12.5);
        assertThat(statistics.column("balance").max()).isEqualTo(30.0);
        assertThat(statistics.indexes()).extracting(IndexStatistics::name).containsExactly("idx_id");
        assertThat(statistics.indexes().get(0).distinctKeys()).isEqualTo(2);
        assertThat(statistics.indexes().get(0).indexedRows()).isEqualTo(3);
    }

    @Test
    void estimatesEqualityRangesAndFallbackSelectivity() {
        TableStatistics statistics = new TableStatistics("items", 100,
                Map.of("amount", new ColumnStatistics("amount", DataType.INTEGER, 10, 0, 99)), List.of());
        CardinalityEstimator estimator = new CardinalityEstimator();

        assertThat(estimator.filterRows(statistics, new FilterCondition(
                "amount", PlanComparisonOperator.EQUAL, 10, DataType.INTEGER))).isEqualTo(10.0);
        assertThat(estimator.filterRows(statistics, new FilterCondition(
                "amount", PlanComparisonOperator.GREATER_THAN, 50, DataType.INTEGER)))
                .isCloseTo(49.4949, within(0.0001));
        assertThat(estimator.filterRows(statistics, new FilterCondition(
                "missing", PlanComparisonOperator.EQUAL, 10, DataType.INTEGER))).isEqualTo(25.0);
    }

    @Test
    void joinEstimateUsesMaximumDistinctCountAndFallback() {
        TableStatistics left = new TableStatistics("left", 100,
                Map.of("key", new ColumnStatistics("key", DataType.INTEGER, 10, 1, 10)), List.of());
        TableStatistics right = new TableStatistics("right", 50,
                Map.of("key", new ColumnStatistics("key", DataType.INTEGER, 20, 1, 20)), List.of());
        CardinalityEstimator estimator = new CardinalityEstimator();

        assertThat(estimator.equalityJoinRows(left, "key", right, "key")).isEqualTo(250.0);
        assertThat(estimator.equalityJoinRows(left, "missing", right, "key")).isEqualTo(500.0);
    }

    @Test
    void usesDeterministicHighAndLowCardinalityFixture() {
        Database database = new Database();
        Table events = new Table("events", new TableSchema(List.of(
                new ColumnDefinition("event_id", DataType.INTEGER),
                new ColumnDefinition("tenant_id", DataType.INTEGER)
        )));
        for (int eventId = 0; eventId < 1_000; eventId++) {
            events.insert(Map.of("event_id", eventId, "tenant_id", eventId % 10));
        }
        database.createTable(events);

        TableStatistics statistics = new StatisticsManager(database).snapshot().table("events");
        CardinalityEstimator estimator = new CardinalityEstimator();

        assertThat(statistics.rowCount()).isEqualTo(1_000);
        assertThat(statistics.column("event_id").distinctValues()).isEqualTo(1_000);
        assertThat(statistics.column("tenant_id").distinctValues()).isEqualTo(10);
        assertThat(estimator.filterRows(statistics, new FilterCondition(
                "event_id", PlanComparisonOperator.EQUAL, 42, DataType.INTEGER))).isEqualTo(1.0);
        assertThat(estimator.filterRows(statistics, new FilterCondition(
                "tenant_id", PlanComparisonOperator.EQUAL, 3, DataType.INTEGER))).isEqualTo(100.0);
    }

    @Test
    void costModelIsDeterministicAndMonotonic() {
        CostModel model = new CostModel();

        assertThat(model.tableScan(100)).isGreaterThan(model.tableScan(10));
        assertThat(model.nestedLoopJoin(10, 20)).isGreaterThan(model.hashJoin(10, 20));
        assertThat(model.indexScan(1000, 5)).isLessThan(model.tableScan(1000));
        assertThat(model.indexScan(1000, 5)).isEqualTo(model.indexScan(1000, 5));
    }
}
