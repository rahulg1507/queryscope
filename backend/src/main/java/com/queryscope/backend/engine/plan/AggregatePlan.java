package com.queryscope.backend.engine.plan;

import java.util.List;
import java.util.Map;

public record AggregatePlan(
        String table,
        List<String> groupBy,
        List<AggregateSpec> aggregates,
        ExecutionPlanNode child,
        Integer inputRows,
        Integer outputRows
) implements ExecutionPlanNode {

    public AggregatePlan(String table, List<String> groupBy, List<AggregateSpec> aggregates, ExecutionPlanNode child) {
        this(table, groupBy, aggregates, child, null, null);
    }

    public AggregatePlan {
        if (table == null || table.isBlank() || groupBy == null || aggregates == null || aggregates.isEmpty() || child == null) {
            throw new IllegalArgumentException("An aggregate plan requires a table, aggregate functions, and one child.");
        }
        groupBy = List.copyOf(groupBy);
        aggregates = List.copyOf(aggregates);
    }

    @Override
    public String type() {
        return "AGGREGATE";
    }

    @Override
    public Map<String, Object> details() {
        return Map.of(
                "groupBy", groupBy,
                "functions", aggregates.stream().map(AggregateSpec::display).toList()
        );
    }

    @Override
    public List<ExecutionPlanNode> children() {
        return List.of(child);
    }

    @Override
    public AggregatePlan withMetrics(int inputRows, int outputRows, List<ExecutionPlanNode> children) {
        if (children == null || children.size() != 1) {
            throw new IllegalArgumentException("Aggregate requires exactly one child.");
        }
        return new AggregatePlan(table, groupBy, aggregates, children.get(0), inputRows, outputRows);
    }
}
