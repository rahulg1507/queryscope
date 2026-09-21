package com.queryscope.backend.engine.optimizer;

import com.queryscope.backend.engine.plan.ExecutionPlanNode;

public record OptimizerCandidate(
        String planType,
        ExecutionPlanNode plan,
        double estimatedRows,
        double estimatedCost
) {
    public OptimizerCandidate {
        if (planType == null || plan == null || !Double.isFinite(estimatedRows)
                || !Double.isFinite(estimatedCost)) {
            throw new IllegalArgumentException("An optimizer candidate requires a plan and finite estimates.");
        }
    }
}
