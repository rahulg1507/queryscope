package com.queryscope.backend.engine.plan;

public record ExecutionPlan(ExecutionPlanNode root) {
    public ExecutionPlan {
        if (root == null) {
            throw new IllegalArgumentException("An execution plan requires a root node.");
        }
    }
}
