package com.queryscope.backend.engine.optimizer;

import com.queryscope.backend.engine.plan.ExecutionMode;
import com.queryscope.backend.engine.plan.ExecutionPlan;

import java.util.List;

public record OptimizationResult(
        ExecutionMode mode,
        ExecutionPlan originalPlan,
        ExecutionPlan optimizedPlan,
        List<OptimizationTrace> rulesApplied
) {
    public OptimizationResult {
        if (mode == null || originalPlan == null || optimizedPlan == null || rulesApplied == null) {
            throw new IllegalArgumentException("An optimization result requires mode, plans, and trace entries.");
        }
        rulesApplied = List.copyOf(rulesApplied);
    }
}
