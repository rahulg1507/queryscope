package com.queryscope.backend.engine.optimizer;

import com.queryscope.backend.engine.plan.AggregatePlan;
import com.queryscope.backend.engine.plan.ExecutionMode;
import com.queryscope.backend.engine.plan.ExecutionPlan;
import com.queryscope.backend.engine.plan.ExecutionPlanNode;
import com.queryscope.backend.engine.plan.FilterPlan;
import com.queryscope.backend.engine.plan.IndexScanPlan;
import com.queryscope.backend.engine.plan.JoinPlan;
import com.queryscope.backend.engine.plan.PlanComparisonOperator;
import com.queryscope.backend.engine.plan.ProjectionPlan;
import com.queryscope.backend.engine.plan.TableScanPlan;
import com.queryscope.backend.engine.storage.ColumnDefinition;
import com.queryscope.backend.engine.storage.ColumnResolution;
import com.queryscope.backend.engine.storage.DataType;
import com.queryscope.backend.engine.storage.Database;
import com.queryscope.backend.engine.storage.Table;
import com.queryscope.backend.engine.index.TableIndex;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic rule-based optimizer. It selects existing physical operators;
 * it never creates indexes, estimates costs, or uses timing measurements.
 */
public final class QueryOptimizer {

    private final Database database;

    public QueryOptimizer(Database database) {
        if (database == null) {
            throw new IllegalArgumentException("An optimizer requires a database.");
        }
        this.database = database;
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
            return new OptimizationResult(mode, plan, plan, trace);
        }
        ExecutionPlanNode optimizedRoot = optimizeNode(plan.root(), trace);
        return new OptimizationResult(mode, plan, new ExecutionPlan(optimizedRoot), trace);
    }

    private ExecutionPlanNode optimizeNode(ExecutionPlanNode node, List<OptimizationTrace> trace) {
        if (node instanceof TableScanPlan || node instanceof IndexScanPlan) {
            return node;
        }
        if (node instanceof ProjectionPlan projection) {
            return new ProjectionPlan(projection.table(), projection.columns(), optimizeNode(projection.child(), trace));
        }
        if (node instanceof AggregatePlan aggregate) {
            return new AggregatePlan(
                    aggregate.table(), aggregate.groupBy(), aggregate.aggregates(),
                    optimizeNode(aggregate.child(), trace)
            );
        }
        if (node instanceof FilterPlan filter) {
            ExecutionPlanNode optimizedChild = optimizeNode(filter.child(), trace);
            TableIndex index = optimizedChild instanceof TableScanPlan ? matchingIndex(filter) : null;
            if (index != null && optimizedChild instanceof TableScanPlan) {
                trace.add(new OptimizationTrace(
                        "MATCHING_INDEX", "USE_INDEX_SCAN",
                        "Index " + index.name() + " exists on " + index.tableName() + "."
                                + index.columnName() + " and supports predicate " + filter.condition().display() + "."
                ));
                return new IndexScanPlan(filter.table(), filter.condition());
            }
            trace.add(new OptimizationTrace(
                    "NO_MATCHING_INDEX", "USE_TABLE_SCAN",
                    "No usable existing index matches predicate " + filter.condition().display() + "."
            ));
            return new FilterPlan(filter.table(), filter.condition(), optimizedChild);
        }
        if (node instanceof JoinPlan join) {
            ExecutionPlanNode left = optimizeNode(join.left(), trace);
            ExecutionPlanNode right = optimizeNode(join.right(), trace);
            if (hashJoinEligible(join)) {
                trace.add(new OptimizationTrace(
                        "EQUALITY_JOIN", "USE_HASH_JOIN",
                        "Join condition " + join.condition().display()
                                + " is an equality join over supported hashable column types."
                ));
                return new JoinPlan(join.leftTable(), join.rightTable(), join.condition(),
                        com.queryscope.backend.engine.plan.JoinStrategy.HASH, left, right);
            }
            trace.add(new OptimizationTrace(
                    "HASH_JOIN_FALLBACK", "USE_NESTED_LOOP_JOIN",
                    "Join condition " + join.condition().display()
                            + " is not eligible for the rule-based hash-join rule."
            ));
            return new JoinPlan(join.leftTable(), join.rightTable(), join.condition(),
                    com.queryscope.backend.engine.plan.JoinStrategy.NESTED_LOOP, left, right);
        }
        throw new IllegalArgumentException("Unsupported plan node for optimization: " + node.type());
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
}
