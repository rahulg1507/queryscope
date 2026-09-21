package com.queryscope.backend.engine.plan;

import java.util.List;
import java.util.Map;

public record TableScanPlan(
        String table,
        Integer inputRows,
        Integer outputRows
) implements ExecutionPlanNode {

    public TableScanPlan(String table) {
        this(table, null, null);
    }

    public TableScanPlan {
        if (table == null || table.isBlank()) {
            throw new IllegalArgumentException("A table scan requires a table name.");
        }
    }

    @Override
    public String type() {
        return "TABLE_SCAN";
    }

    @Override
    public Map<String, Object> details() {
        return Map.of("table", table);
    }

    @Override
    public List<ExecutionPlanNode> children() {
        return List.of();
    }

    @Override
    public TableScanPlan withMetrics(int inputRows, int outputRows, List<ExecutionPlanNode> children) {
        if (children != null && !children.isEmpty()) {
            throw new IllegalArgumentException("TableScan cannot have children.");
        }
        return new TableScanPlan(table, inputRows, outputRows);
    }
}
