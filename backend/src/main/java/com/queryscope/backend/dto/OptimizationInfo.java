package com.queryscope.backend.dto;

import com.queryscope.backend.engine.optimizer.OptimizationResult;
import com.queryscope.backend.engine.plan.ExecutionPlanNodeDto;

import java.util.List;

public record OptimizationInfo(
        String mode,
        ExecutionPlanNodeDto originalPlan,
        ExecutionPlanNodeDto optimizedPlan,
        List<OptimizationTraceDto> rulesApplied,
        List<OptimizerCandidateDto> candidates,
        String selectedPlan,
        Double estimatedRows,
        Double estimatedCost,
        String selectionReason
) {
    public OptimizationInfo {
        rulesApplied = rulesApplied == null ? List.of() : List.copyOf(rulesApplied);
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }

    public static OptimizationInfo from(OptimizationResult result, ExecutionPlanNodeDto executedPlan) {
        return new OptimizationInfo(
                result.mode().name(),
                ExecutionPlanNodeDto.from(result.originalPlan().root()),
                executedPlan,
                result.rulesApplied().stream()
                        .map(trace -> new OptimizationTraceDto(trace.rule(), trace.decision(), trace.reason()))
                        .toList(),
                result.candidates().stream()
                        .map(candidate -> new OptimizerCandidateDto(
                                candidate.planType(), candidate.estimatedRows(), candidate.estimatedCost()))
                        .toList(),
                result.selectedPlan(),
                result.estimatedRows(),
                result.estimatedCost(),
                result.selectionReason()
        );
    }
}
