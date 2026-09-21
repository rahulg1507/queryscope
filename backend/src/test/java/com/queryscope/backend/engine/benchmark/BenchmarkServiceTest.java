package com.queryscope.backend.engine.benchmark;

import com.queryscope.backend.dto.BenchmarkResultDto;
import com.queryscope.backend.engine.execution.QueryExecutionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BenchmarkServiceTest {

    private final BenchmarkService service = new BenchmarkService();

    @Test
    void scanBenchmarkComparesEquivalentTableAndIndexResults() {
        BenchmarkResultDto result = service.run(BenchmarkScenario.INDEX_EQUALITY, BenchmarkDatasetSize.SMALL);

        assertThat(result.resultsEquivalent()).isTrue();
        assertThat(result.rowsInvolved()).isEqualTo(1_000);
        assertThat(result.comparisons()).extracting(comparison -> comparison.strategy())
                .containsExactly("TABLE_SCAN", "INDEX_SCAN");
        assertThat(result.comparisons().get(0).actualMetrics().get("rowsScanned")).isEqualTo(1_000);
        assertThat(result.comparisons().get(1).actualMetrics().get("indexLookups")).isEqualTo(1);
        assertThat(result.comparisons().get(1).actualMetrics().get("leafEntriesVisited"))
                .isInstanceOf(Number.class);
    }

    @Test
    void joinBenchmarkIsRepeatableAndReportsOperationCounts() {
        BenchmarkResultDto first = service.run(BenchmarkScenario.JOIN_SMALL, BenchmarkDatasetSize.SMALL);
        BenchmarkResultDto second = service.run(BenchmarkScenario.JOIN_SMALL, BenchmarkDatasetSize.SMALL);

        assertThat(first.resultsEquivalent()).isTrue();
        assertThat(first.comparisons()).isEqualTo(second.comparisons());
        assertThat(first.comparisons().get(0).actualMetrics().get("comparisons")).isEqualTo(100_000);
        assertThat(first.comparisons().get(1).actualMetrics().get("hashLookups")).isEqualTo(1_000);
        assertThat(first.comparisons().get(0).actualRowsReturned()).isEqualTo(1_000);
    }

    @Test
    void optimizerBenchmarksExposeCandidatesAndEstimationError() {
        BenchmarkResultDto scan = service.run(BenchmarkScenario.OPTIMIZER_SCAN, BenchmarkDatasetSize.MEDIUM);
        BenchmarkResultDto join = service.run(BenchmarkScenario.OPTIMIZER_JOIN, BenchmarkDatasetSize.SMALL);

        assertThat(scan.optimizer()).isNotNull();
        assertThat(scan.optimizer().candidates()).hasSize(2);
        assertThat(scan.optimizer().selectedPlan()).isEqualTo("INDEX_SCAN");
        assertThat(scan.optimizer().estimationError().actualRows()).isEqualTo(10);
        assertThat(join.optimizer().selectedPlan()).isEqualTo("HASH_JOIN");
        assertThat(join.optimizer().candidates()).extracting(candidate -> candidate.planType())
                .containsExactly("NESTED_LOOP_JOIN", "HASH_JOIN");
    }

    @Test
    void estimationErrorHandlesZeroActualRowsWithoutDivisionByZero() {
        assertThat(EstimationErrorCalculator.calculate(2.5, 0).percentageError()).isNull();
        assertThat(EstimationErrorCalculator.calculate(2.5, 0).absoluteError()).isEqualTo(2.5);
    }

    @Test
    void invalidScenarioAndDatasetSizeAreRejected() {
        assertThatThrownBy(() -> BenchmarkScenario.from("timing_everything"))
                .isInstanceOf(QueryExecutionException.class)
                .hasMessageContaining("Invalid benchmark scenario");
        assertThatThrownBy(() -> BenchmarkDatasetSize.from("HUGE"))
                .isInstanceOf(QueryExecutionException.class)
                .hasMessageContaining("Invalid benchmark dataset size");
    }
}
