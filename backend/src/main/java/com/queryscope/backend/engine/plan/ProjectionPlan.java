package com.queryscope.backend.engine.plan;

import java.util.List;
import java.util.Map;

public record ProjectionPlan(
        String table,
        List<String> columns,
        ExecutionPlanNode child,
        Integer inputRows,
        Integer outputRows
) implements ExecutionPlanNode {

    public ProjectionPlan(String table, List<String> columns, ExecutionPlanNode child) {
        this(table, columns, child, null, null);
    }

    public ProjectionPlan {
        if (table == null || table.isBlank() || columns == null || columns.isEmpty() || child == null) {
            throw new IllegalArgumentException("A projection plan requires a table, columns, and child.");
        }
        columns = List.copyOf(columns);
    }

    @Override
    public String type() {
        return "PROJECTION";
    }

    @Override
    public Map<String, Object> details() {
        return Map.of("columns", columns);
    }

    @Override
    public List<ExecutionPlanNode> children() {
        return List.of(child);
    }

    @Override
    public ProjectionPlan withMetrics(int inputRows, int outputRows, List<ExecutionPlanNode> children) {
        if (children == null || children.size() != 1) {
            throw new IllegalArgumentException("Projection requires exactly one child.");
        }
        return new ProjectionPlan(table, columns, children.get(0), inputRows, outputRows);
    }
}
