package com.queryscope.backend.engine.optimizer;

import com.queryscope.backend.engine.plan.AggregatePlan;
import com.queryscope.backend.engine.plan.ExecutionMode;
import com.queryscope.backend.engine.plan.ExecutionPlan;
import com.queryscope.backend.engine.plan.ExecutionPlanNode;
import com.queryscope.backend.engine.plan.FilterPlan;
import com.queryscope.backend.engine.plan.IndexScanPlan;
import com.queryscope.backend.engine.plan.JoinPlan;
import com.queryscope.backend.engine.plan.JoinStrategy;
import com.queryscope.backend.engine.plan.PlanComparisonOperator;
import com.queryscope.backend.engine.plan.ProjectionPlan;
import com.queryscope.backend.engine.plan.TableScanPlan;
import com.queryscope.backend.engine.storage.ColumnDefinition;
import com.queryscope.backend.engine.storage.ColumnResolution;
import com.queryscope.backend.engine.storage.DataType;
import com.queryscope.backend.engine.storage.Database;
import com.queryscope.backend.engine.storage.Table;
import com.queryscope.backend.engine.index.TableIndex;
import com.queryscope.backend.engine.statistics.CardinalityEstimator;
import com.queryscope.backend.engine.statistics.StatisticsConfig;
import com.queryscope.backend.engine.statistics.StatisticsManager;
import com.queryscope.backend.engine.statistics.StatisticsSnapshot;
import com.queryscope.backend.engine.statistics.TableStatistics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Deterministic cost-based optimizer over the existing physical operators. */
public final class QueryOptimizer {

    private final Database database;
    private final StatisticsManager statisticsManager;
    private final CardinalityEstimator cardinalityEstimator = new CardinalityEstimator();
    private final CostModel costModel = new CostModel();

    public QueryOptimizer(Database database) {
        if (database == null) {
            throw new IllegalArgumentException("An optimizer requires a database.");
        }
        this.database = database;
        this.statisticsManager = new StatisticsManager(database);
    }

    public OptimizationResult optimize(ExecutionPlan plan, ExecutionMode mode) {
        if (plan == null || mode == null) {
            throw new IllegalArgumentException("Optimization requires a plan and execution mode.");
        }
        List<OptimizationTrace> trace = new ArrayList<>();
        if (mode == ExecutionMode.MANUAL) {
            trace.add(new OptimizationTrace(
                    "MANUAL_STRATEGIES", "PRESERVE_PLAN",
                    "Manual mode preserves the requested scan and join strategies."
            ));
            return new OptimizationResult(mode, plan, plan, trace, List.of(), physicalType(plan.root()), null, null,
                    "Manual mode bypasses cost-based candidate selection.");
        }

        // One snapshot keeps all estimates in a planning request consistent.
        StatisticsSnapshot snapshot = statisticsManager.snapshot();
        List<OptimizerCandidate> candidates = new ArrayList<>();
        EstimateAndPlan optimized = optimizeNode(plan.root(), snapshot, trace, candidates);
        return new OptimizationResult(
                mode, plan, new ExecutionPlan(optimized.plan()), trace, candidates,
                optimized.selectedPlan(), optimized.rows(), optimized.cost(), optimized.reason()
        );
    }

    private EstimateAndPlan optimizeNode(
            ExecutionPlanNode node, StatisticsSnapshot snapshot,
            List<OptimizationTrace> trace, List<OptimizerCandidate> candidates
    ) {
        if (node instanceof TableScanPlan tableScan) {
            return estimate(tableScan, snapshot);
        }
        if (node instanceof IndexScanPlan indexScan) {
            return estimate(indexScan, snapshot);
        }
        if (node instanceof ProjectionPlan projection) {
            EstimateAndPlan child = optimizeNode(projection.child(), snapshot, trace, candidates);
            ProjectionPlan optimized = new ProjectionPlan(projection.table(), projection.columns(), child.plan());
            return new EstimateAndPlan(optimized, child.rows(),
                    child.cost() + costModel.projection(child.rows()), "Projection cost retained.", child.selectedPlan());
        }
        if (node instanceof AggregatePlan aggregate) {
            EstimateAndPlan child = optimizeNode(aggregate.child(), snapshot, trace, candidates);
            AggregatePlan optimized = new AggregatePlan(
                    aggregate.table(), aggregate.groupBy(), aggregate.aggregates(), child.plan());
            return new EstimateAndPlan(optimized, Math.max(1d, child.rows()),
                    child.cost() + costModel.aggregate(child.rows()), "Aggregate cost retained.", child.selectedPlan());
        }
        if (node instanceof FilterPlan filter) {
            EstimateAndPlan child = optimizeNode(filter.child(), snapshot, trace, candidates);
            TableIndex index = child.plan() instanceof TableScanPlan ? matchingIndex(filter) : null;
            FilterPlan tablePlan = new FilterPlan(filter.table(), filter.condition(), child.plan());
            EstimateAndPlan tableEstimate = estimate(tablePlan, snapshot);
            if (index == null) {
                trace.add(new OptimizationTrace(
                        "NO_MATCHING_INDEX", "USE_TABLE_SCAN",
                        "No matching index exists for predicate on "
                                + simpleName(filter.condition().column()) + "."
                ));
                candidates.add(candidate(tableEstimate));
                return tableEstimate;
            }

            IndexScanPlan indexPlan = new IndexScanPlan(filter.table(), filter.condition());
            EstimateAndPlan indexEstimate = estimate(indexPlan, snapshot);
            candidates.add(candidate(tableEstimate));
            candidates.add(candidate(indexEstimate));
            trace.add(new OptimizationTrace(
                    "MATCHING_INDEX", "USE_INDEX_SCAN",
                    "Index " + index.name() + " exists on " + index.tableName() + "."
                            + index.columnName() + " and supports predicate " + filter.condition().display() + "."
            ));
            EstimateAndPlan selected = choose(List.of(tableEstimate, indexEstimate), "INDEX_SCAN");
            trace.add(new OptimizationTrace(
                    "COST_BASED_SCAN", "USE_" + selected.plan().type(),
                    "Selected " + selected.plan().type() + " because its estimated cost is "
                            + format(selected.cost()) + " versus "
                            + candidateType(other(selected, tableEstimate, indexEstimate)) + " at "
                            + format(other(selected, tableEstimate, indexEstimate).cost()) + "."
            ));
            return selected;
        }
        if (node instanceof JoinPlan join) {
            EstimateAndPlan left = optimizeNode(join.left(), snapshot, trace, candidates);
            EstimateAndPlan right = optimizeNode(join.right(), snapshot, trace, candidates);
            JoinPlan nestedPlan = new JoinPlan(join.leftTable(), join.rightTable(), join.condition(),
                    JoinStrategy.NESTED_LOOP, left.plan(), right.plan());
            EstimateAndPlan nested = estimate(nestedPlan, snapshot);
            if (!hashJoinEligible(join)) {
                trace.add(new OptimizationTrace(
                        "HASH_JOIN_FALLBACK", "USE_NESTED_LOOP_JOIN",
                        "Join condition " + join.condition().display()
                                + " is not eligible for the hash-join candidate."
                ));
                candidates.add(candidate(nested));
                return nested;
            }
            JoinPlan hashPlan = new JoinPlan(join.leftTable(), join.rightTable(), join.condition(),
                    JoinStrategy.HASH, left.plan(), right.plan());
            EstimateAndPlan hash = estimate(hashPlan, snapshot);
            candidates.add(candidate(nested));
            candidates.add(candidate(hash));
            trace.add(new OptimizationTrace(
                    "EQUALITY_JOIN", "CONSIDER_HASH_JOIN",
                    "Join condition " + join.condition().display()
                            + " is eligible for the hash-join candidate."
            ));
            EstimateAndPlan selected = choose(List.of(nested, hash), "HASH_JOIN");
            trace.add(new OptimizationTrace(
                    "COST_BASED_JOIN", "USE_" + selected.plan().type(),
                    "Selected " + selected.plan().type() + " because its estimated cost is "
                            + format(selected.cost()) + " versus "
                            + candidateType(other(selected, nested, hash)) + " at "
                            + format(other(selected, nested, hash).cost()) + "."
            ));
            return selected;
        }
        throw new IllegalArgumentException("Unsupported plan node for optimization: " + node.type());
    }

    private EstimateAndPlan estimate(ExecutionPlanNode node, StatisticsSnapshot snapshot) {
        if (node instanceof TableScanPlan scan) {
            TableStatistics table = snapshot.table(scan.table());
            return new EstimateAndPlan(node, table.rowCount(), costModel.tableScan(table.rowCount()),
                    "Table scan estimates all rows.", node.type());
        }
        if (node instanceof IndexScanPlan scan) {
            TableStatistics table = snapshot.table(scan.table());
            double rows = cardinalityEstimator.filterRows(table, scan.condition());
            int indexedRows = table.indexes().stream()
                    .filter(index -> index.column().equalsIgnoreCase(simpleName(scan.condition().column())))
                    .mapToInt(com.queryscope.backend.engine.statistics.IndexStatistics::indexedRows)
                    .findFirst().orElse((int) table.rowCount());
            return new EstimateAndPlan(node, rows, costModel.indexScan(indexedRows, rows),
                    "Index lookup estimates matching predicate rows.", node.type());
        }
        if (node instanceof FilterPlan filter) {
            EstimateAndPlan child = estimate(filter.child(), snapshot);
            TableStatistics table = tableOrNull(snapshot, filter.table());
            double rows = table == null
                    ? child.rows() * StatisticsConfig.DEFAULT_FILTER_SELECTIVITY
                    : cardinalityEstimator.filterRows(table, filter.condition());
            return new EstimateAndPlan(node, clamp(rows, child.rows()),
                    child.cost() + costModel.filter(child.rows()),
                    "Filter applies default/statistical selectivity.", "TABLE_SCAN");
        }
        if (node instanceof ProjectionPlan projection) {
            EstimateAndPlan child = estimate(projection.child(), snapshot);
            return new EstimateAndPlan(node, child.rows(), child.cost() + costModel.projection(child.rows()),
                    "Projection retains input cardinality.", child.selectedPlan());
        }
        if (node instanceof AggregatePlan aggregate) {
            EstimateAndPlan child = estimate(aggregate.child(), snapshot);
            return new EstimateAndPlan(node, Math.max(1d, child.rows()),
                    child.cost() + costModel.aggregate(child.rows()), "Aggregate groups input rows.", child.selectedPlan());
        }
        if (node instanceof JoinPlan join) {
            EstimateAndPlan left = estimate(join.left(), snapshot);
            EstimateAndPlan right = estimate(join.right(), snapshot);
            double rows = join.condition().operator() == PlanComparisonOperator.EQUAL
                    ? equalityJoinRows(snapshot, join, left.rows(), right.rows())
                    : left.rows() * right.rows() * StatisticsConfig.DEFAULT_JOIN_SELECTIVITY;
            double joinCost = join.strategy() == JoinStrategy.HASH
                    ? costModel.hashJoin(left.rows(), right.rows())
                    : costModel.nestedLoopJoin(left.rows(), right.rows());
            return new EstimateAndPlan(node, rows, left.cost() + right.cost() + joinCost,
                    "Join cardinality uses column statistics when available.", node.type());
        }
        throw new IllegalArgumentException("Unsupported plan node for estimation: " + node.type());
    }

    private double equalityJoinRows(StatisticsSnapshot snapshot, JoinPlan join, double leftRows, double rightRows) {
        TableStatistics left = tableOrNull(snapshot, join.leftTable());
        TableStatistics right = tableOrNull(snapshot, join.rightTable());
        if (left == null || right == null) {
            return leftRows * rightRows * StatisticsConfig.DEFAULT_JOIN_SELECTIVITY;
        }
        return cardinalityEstimator.equalityJoinRows(left, join.condition().leftColumn(), right,
                join.condition().rightColumn());
    }

    private TableIndex matchingIndex(FilterPlan filter) {
        if (filter.condition().operator() == PlanComparisonOperator.NOT_EQUAL) {
            return null;
        }
        Table table = database.requireTable(filter.table());
        ColumnResolution column = table.schema().resolveColumn(filter.condition().column(), table.name());
        TableIndex index = table.indexForColumn(column.actualName());
        if (index == null || index.keyType() != filter.condition().valueType()) {
            return null;
        }
        return index;
    }

    private boolean hashJoinEligible(JoinPlan join) {
        if (join.condition().operator() != PlanComparisonOperator.EQUAL) {
            return false;
        }
        Table left = database.requireTable(join.leftTable());
        Table right = database.requireTable(join.rightTable());
        ColumnDefinition leftColumn = left.schema()
                .resolveColumn(join.condition().leftColumn(), left.name()).column();
        ColumnDefinition rightColumn = right.schema()
                .resolveColumn(join.condition().rightColumn(), right.name()).column();
        return leftColumn.type() == rightColumn.type() && hashable(leftColumn.type());
    }

    private static boolean hashable(DataType type) {
        return type == DataType.INTEGER || type == DataType.STRING || type == DataType.BOOLEAN;
    }

    private static OptimizerCandidate candidate(EstimateAndPlan estimate) {
        return new OptimizerCandidate(estimate.selectedPlan(), estimate.plan(), estimate.rows(), estimate.cost());
    }

    private static EstimateAndPlan choose(List<EstimateAndPlan> options, String tieWinner) {
        return options.stream().min(Comparator.comparingDouble(EstimateAndPlan::cost)
                .thenComparing(option -> option.plan().type().equals(tieWinner) ? 0 : 1)).orElseThrow();
    }

    private static EstimateAndPlan other(EstimateAndPlan selected, EstimateAndPlan first, EstimateAndPlan second) {
        return selected == first ? second : first;
    }

    private static String candidateType(EstimateAndPlan estimate) {
        return estimate.selectedPlan();
    }

    private static TableStatistics tableOrNull(StatisticsSnapshot snapshot, String table) {
        try {
            return snapshot.table(table);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static double clamp(double value, double maximum) {
        return Math.max(0d, Math.min(Math.max(0d, maximum), value));
    }

    private static String simpleName(String value) {
        int separator = value.lastIndexOf('.');
        return separator < 0 ? value : value.substring(separator + 1);
    }

    private static String physicalType(ExecutionPlanNode node) {
        if (node instanceof JoinPlan join) {
            return join.type();
        }
        if (node instanceof IndexScanPlan || node instanceof TableScanPlan) {
            return node.type();
        }
        if (!node.children().isEmpty()) {
            return physicalType(node.children().get(0));
        }
        return node.type();
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private record EstimateAndPlan(
            ExecutionPlanNode plan, double rows, double cost, String reason, String selectedPlan
    ) {
    }
}
