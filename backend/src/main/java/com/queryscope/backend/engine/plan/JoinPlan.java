package com.queryscope.backend.engine.plan;

import java.util.List;
import java.util.Map;

public record JoinPlan(
        String leftTable,
        String rightTable,
        JoinCondition condition,
        JoinStrategy strategy,
        ExecutionPlanNode left,
        ExecutionPlanNode right,
        Integer inputRows,
        Integer outputRows
) implements ExecutionPlanNode {

    public JoinPlan(
            String leftTable,
            String rightTable,
            JoinCondition condition,
            JoinStrategy strategy,
            ExecutionPlanNode left,
            ExecutionPlanNode right
    ) {
        this(leftTable, rightTable, condition, strategy, left, right, null, null);
    }

    public JoinPlan {
        if (leftTable == null || leftTable.isBlank() || rightTable == null || rightTable.isBlank()
                || condition == null || strategy == null || left == null || right == null) {
            throw new IllegalArgumentException("A join requires two tables, a condition, a strategy, and two children.");
        }
    }

    @Override
    public String type() {
        return strategy.planType();
    }

    @Override
    public Map<String, Object> details() {
        return Map.of("condition", condition.display(), "strategy", strategy.name());
    }

    @Override
    public List<ExecutionPlanNode> children() {
        return List.of(left, right);
    }

    @Override
    public JoinPlan withMetrics(int inputRows, int outputRows, List<ExecutionPlanNode> children) {
        if (children == null || children.size() != 2) {
            throw new IllegalArgumentException("A join requires exactly two children.");
        }
        return new JoinPlan(leftTable, rightTable, condition, strategy, children.get(0), children.get(1), inputRows, outputRows);
    }
}
