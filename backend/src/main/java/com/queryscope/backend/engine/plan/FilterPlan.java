package com.queryscope.backend.engine.plan;

import java.util.List;
import java.util.Map;

public record FilterPlan(
        String table,
        FilterCondition condition,
        ExecutionPlanNode child,
        Integer inputRows,
        Integer outputRows
) implements ExecutionPlanNode {

    public FilterPlan(String table, FilterCondition condition, ExecutionPlanNode child) {
        this(table, condition, child, null, null);
    }

    public FilterPlan {
        if (table == null || table.isBlank() || condition == null || child == null) {
            throw new IllegalArgumentException("A filter plan requires a table, condition, and child.");
        }
    }

    @Override
    public String type() {
        return "FILTER";
    }

    @Override
    public Map<String, Object> details() {
        return Map.of("condition", condition.display());
    }

    @Override
    public List<ExecutionPlanNode> children() {
        return List.of(child);
    }

    @Override
    public FilterPlan withMetrics(int inputRows, int outputRows, List<ExecutionPlanNode> children) {
        if (children == null || children.size() != 1) {
            throw new IllegalArgumentException("Filter requires exactly one child.");
        }
        return new FilterPlan(table, condition, children.get(0), inputRows, outputRows);
    }
}
