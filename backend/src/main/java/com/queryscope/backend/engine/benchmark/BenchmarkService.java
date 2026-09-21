package com.queryscope.backend.engine.benchmark;

import com.queryscope.backend.dto.BenchmarkComparisonDto;
import com.queryscope.backend.dto.BenchmarkEstimationErrorDto;
import com.queryscope.backend.dto.BenchmarkOptimizerDto;
import com.queryscope.backend.dto.BenchmarkResultDto;
import com.queryscope.backend.dto.OptimizerCandidateDto;
import com.queryscope.backend.engine.ast.SelectStatement;
import com.queryscope.backend.engine.execution.InMemoryQueryExecutor;
import com.queryscope.backend.engine.execution.QueryResult;
import com.queryscope.backend.engine.parser.Lexer;
import com.queryscope.backend.engine.parser.Parser;
import com.queryscope.backend.engine.plan.ExecutionMode;
import com.queryscope.backend.engine.plan.ExecutionPlanNodeDto;
import com.queryscope.backend.engine.plan.JoinStrategy;
import com.queryscope.backend.engine.plan.ScanStrategy;
import com.queryscope.backend.engine.storage.Database;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Runs predefined deterministic experiments through the production query path. */
public final class BenchmarkService {

    private static final String SCAN_QUERY = "SELECT id, amount FROM expenses WHERE %s";
    private static final String JOIN_QUERY =
            "SELECT users.id, expenses.amount FROM users JOIN expenses ON users.id = expenses.user_id";

    private final BenchmarkDatasetFactory datasetFactory;

    public BenchmarkService() {
        this(new BenchmarkDatasetFactory());
    }

    public BenchmarkService(BenchmarkDatasetFactory datasetFactory) {
        this.datasetFactory = datasetFactory;
    }

    public BenchmarkResultDto run(BenchmarkScenario scenario, BenchmarkDatasetSize size) {
        if (scenario == null || size == null) {
            throw new IllegalArgumentException("A benchmark requires a scenario and dataset size.");
        }
        if (scenario.scanScenario()) {
            return runScan(scenario, size);
        }
        if (scenario.joinScenario()) {
            return runJoin(scenario, size);
        }
        throw new IllegalArgumentException("Unsupported benchmark scenario '" + scenario + "'.");
    }

    private BenchmarkResultDto runScan(BenchmarkScenario scenario, BenchmarkDatasetSize size) {
        Database database = datasetFactory.create(size, true);
        InMemoryQueryExecutor executor = new InMemoryQueryExecutor(database);
        String predicate = switch (scenario) {
            case INDEX_EQUALITY, OPTIMIZER_SCAN -> "amount = 499";
            case INDEX_RANGE_SELECTIVE -> "amount > 400";
            case INDEX_RANGE_BROAD -> "amount > 10";
            default -> throw new IllegalArgumentException("Scenario is not a scan benchmark: " + scenario);
        };
        SelectStatement statement = parse(String.format(SCAN_QUERY, predicate));
        QueryResult table = executor.execute(statement, ExecutionMode.MANUAL, JoinStrategy.NESTED_LOOP, ScanStrategy.TABLE);
        QueryResult index = executor.execute(statement, ExecutionMode.MANUAL, JoinStrategy.NESTED_LOOP, ScanStrategy.INDEX);
        ensureEquivalent(table, index);

        BenchmarkOptimizerDto optimizer = null;
        if (scenario == BenchmarkScenario.OPTIMIZER_SCAN) {
            QueryResult automatic = executor.execute(statement, ExecutionMode.AUTO, JoinStrategy.NESTED_LOOP, ScanStrategy.TABLE);
            ensureEquivalent(table, automatic);
            optimizer = optimizer(automatic);
        }

        List<BenchmarkComparisonDto> comparisons = List.of(
                comparison("TABLE_SCAN", table, "TABLE_SCAN"),
                comparison("INDEX_SCAN", index, "INDEX_SCAN")
        );
        return new BenchmarkResultDto(
                scenario.name(), size.name(), size.expenseRows(),
                List.of("TABLE_SCAN", "INDEX_SCAN"), true, comparisons, optimizer,
                scanExplanation(comparisons)
        );
    }

    private BenchmarkResultDto runJoin(BenchmarkScenario scenario, BenchmarkDatasetSize size) {
        Database database = datasetFactory.create(size, false);
        InMemoryQueryExecutor executor = new InMemoryQueryExecutor(database);
        SelectStatement statement = parse(JOIN_QUERY);
        QueryResult nested = executor.execute(statement, ExecutionMode.MANUAL, JoinStrategy.NESTED_LOOP, ScanStrategy.TABLE);
        QueryResult hash = executor.execute(statement, ExecutionMode.MANUAL, JoinStrategy.HASH, ScanStrategy.TABLE);
        ensureEquivalent(nested, hash);

        BenchmarkOptimizerDto optimizer = null;
        if (scenario == BenchmarkScenario.OPTIMIZER_JOIN) {
            QueryResult automatic = executor.execute(statement, ExecutionMode.AUTO, JoinStrategy.NESTED_LOOP, ScanStrategy.TABLE);
            ensureEquivalent(nested, automatic);
            optimizer = optimizer(automatic);
        }

        List<BenchmarkComparisonDto> comparisons = List.of(
                comparison("NESTED_LOOP_JOIN", nested, "NESTED_LOOP_JOIN"),
                comparison("HASH_JOIN", hash, "HASH_JOIN")
        );
        return new BenchmarkResultDto(
                scenario.name(), size.name(), size.userRows() + size.expenseRows(),
                List.of("NESTED_LOOP_JOIN", "HASH_JOIN"), true, comparisons, optimizer,
                joinExplanation(comparisons)
        );
    }

    private BenchmarkComparisonDto comparison(String strategy, QueryResult result, String nodeType) {
        ExecutionPlanNodeDto node = find(result.executionPlan(), nodeType);
        Map<String, Object> metrics = new LinkedHashMap<>();
        if (node != null) {
            metrics.putAll(node.details());
        }
        metrics.putIfAbsent("rowsScanned", result.metrics().rowsScanned());
        metrics.put("rowsReturned", result.rowCount());
        return new BenchmarkComparisonDto(strategy, nodeType, result.rowCount(), metrics);
    }

    private BenchmarkOptimizerDto optimizer(QueryResult result) {
        if (result.optimization() == null) {
            return null;
        }
        List<OptimizerCandidateDto> candidates = result.optimization().candidates();
        Double estimatedRows = result.optimization().estimatedRows();
        int actualRows = result.rowCount();
        BenchmarkEstimationErrorDto error = estimatedRows == null
                ? null : EstimationErrorCalculator.calculate(estimatedRows, actualRows);
        ExecutionPlanNodeDto actualNode = find(result.executionPlan(), result.optimization().selectedPlan());
        Map<String, Object> actualMetrics = new LinkedHashMap<>();
        if (actualNode != null) {
            actualMetrics.putAll(actualNode.details());
        }
        actualMetrics.put("rowsScanned", result.metrics().rowsScanned());
        actualMetrics.put("rowsReturned", actualRows);
        return new BenchmarkOptimizerDto(
                result.optimization().selectedPlan(), estimatedRows, result.optimization().estimatedCost(),
                result.optimization().selectionReason(), candidates, error, actualRows, actualMetrics
        );
    }

    private static String scanExplanation(List<BenchmarkComparisonDto> comparisons) {
        int tableRows = number(comparisons.get(0).actualMetrics().get("rowsScanned"));
        int indexEntries = number(comparisons.get(1).actualMetrics().get("leafEntriesVisited"));
        return "TableScan inspected " + tableRows + " rows; IndexScan visited " + indexEntries
                + " index entries for the same logical result. These are operation counts, not timings.";
    }

    private static String joinExplanation(List<BenchmarkComparisonDto> comparisons) {
        int comparisonsCount = number(comparisons.get(0).actualMetrics().get("comparisons"));
        int hashOperations = number(comparisons.get(1).actualMetrics().get("buildRows"))
                + number(comparisons.get(1).actualMetrics().get("probeRows"));
        return "NestedLoopJoin performed " + comparisonsCount + " pair comparisons, while HashJoin processed "
                + hashOperations + " build/probe rows for the same logical result.";
    }

    private static int number(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static SelectStatement parse(String sql) {
        return new Parser(new Lexer(sql).tokenize()).parse();
    }

    private static void ensureEquivalent(QueryResult expected, QueryResult actual) {
        if (!expected.columns().equals(actual.columns()) || canonicalRows(expected).equals(canonicalRows(actual)) == false) {
            throw new IllegalStateException("Benchmark strategies produced different logical results.");
        }
    }

    private static List<String> canonicalRows(QueryResult result) {
        return result.rows().stream()
                .map(String::valueOf)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private static ExecutionPlanNodeDto find(ExecutionPlanNodeDto node, String type) {
        if (node == null) {
            return null;
        }
        if (node.type().equals(type)) {
            return node;
        }
        for (ExecutionPlanNodeDto child : node.children()) {
            ExecutionPlanNodeDto found = find(child, type);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
