package com.queryscope.backend.engine.plan;

import java.util.List;
import java.util.Map;

public record NestedLoopJoinPlan(
        String leftTable,
        String rightTable,
        JoinCondition condition,
        ExecutionPlanNode left,
        ExecutionPlanNode right,
        Integer inputRows,
        Integer outputRows
) implements ExecutionPlanNode {

    public NestedLoopJoinPlan(
            String leftTable,
            String rightTable,
            JoinCondition condition,
            ExecutionPlanNode left,
            ExecutionPlanNode right
    ) {
        this(leftTable, rightTable, condition, left, right, null, null);
    }

    public NestedLoopJoinPlan {
        if (leftTable == null || leftTable.isBlank() || rightTable == null || rightTable.isBlank()
                || condition == null || left == null || right == null) {
            throw new IllegalArgumentException("A nested-loop join requires two tables, a condition, and two children.");
        }
    }

    @Override
    public String type() {
        return "NESTED_LOOP_JOIN";
    }

    @Override
    public Map<String, Object> details() {
        return Map.of("condition", condition.display());
    }

    @Override
    public List<ExecutionPlanNode> children() {
        return List.of(left, right);
    }

    @Override
    public NestedLoopJoinPlan withMetrics(int inputRows, int outputRows, List<ExecutionPlanNode> children) {
        if (children == null || children.size() != 2) {
            throw new IllegalArgumentException("Nested-loop join requires exactly two children.");
        }
        return new NestedLoopJoinPlan(leftTable, rightTable, condition, children.get(0), children.get(1), inputRows, outputRows);
    }
}
