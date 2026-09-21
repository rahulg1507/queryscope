package com.queryscope.backend.engine.plan;

import java.util.List;
import java.util.Map;

public record IndexScanPlan(
        String table,
        FilterCondition condition,
        Integer inputRows,
        Integer outputRows
) implements ExecutionPlanNode {

    public IndexScanPlan(String table, FilterCondition condition) {
        this(table, condition, null, null);
    }

    public IndexScanPlan {
        if (table == null || table.isBlank() || condition == null) {
            throw new IllegalArgumentException("An index scan requires a table and predicate.");
        }
    }

    @Override
    public String type() {
        return "INDEX_SCAN";
    }

    @Override
    public Map<String, Object> details() {
        return Map.of("table", table, "predicate", condition.display());
    }

    @Override
    public List<ExecutionPlanNode> children() {
        return List.of();
    }

    @Override
    public IndexScanPlan withMetrics(int inputRows, int outputRows, List<ExecutionPlanNode> children) {
        if (children != null && !children.isEmpty()) {
            throw new IllegalArgumentException("Index scan cannot have children.");
        }
        return new IndexScanPlan(table, condition, inputRows, outputRows);
    }
}
