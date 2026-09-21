package com.queryscope.backend.engine.optimizer;

import com.queryscope.backend.engine.plan.ExecutionMode;
import com.queryscope.backend.engine.plan.ExecutionPlan;

import java.util.List;

public record OptimizationResult(
        ExecutionMode mode,
        ExecutionPlan originalPlan,
        ExecutionPlan optimizedPlan,
        List<OptimizationTrace> rulesApplied,
        List<OptimizerCandidate> candidates,
        String selectedPlan,
        Double estimatedRows,
        Double estimatedCost,
        String selectionReason
) {
    public OptimizationResult(
            ExecutionMode mode,
            ExecutionPlan originalPlan,
            ExecutionPlan optimizedPlan,
            List<OptimizationTrace> rulesApplied
    ) {
        this(mode, originalPlan, optimizedPlan, rulesApplied, List.of(), optimizedPlan.root().type(), null, null, null);
    }

    public OptimizationResult {
        if (mode == null || originalPlan == null || optimizedPlan == null || rulesApplied == null
                || candidates == null) {
            throw new IllegalArgumentException("An optimization result requires mode, plans, and trace entries.");
        }
        rulesApplied = List.copyOf(rulesApplied);
        candidates = List.copyOf(candidates);
    }
}
