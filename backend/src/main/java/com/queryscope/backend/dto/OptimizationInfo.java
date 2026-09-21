package com.queryscope.backend.dto;

import com.queryscope.backend.engine.optimizer.OptimizationResult;
import com.queryscope.backend.engine.plan.ExecutionPlanNodeDto;

import java.util.List;

public record OptimizationInfo(
        String mode,
        ExecutionPlanNodeDto originalPlan,
        ExecutionPlanNodeDto optimizedPlan,
        List<OptimizationTraceDto> rulesApplied
) {
    public OptimizationInfo {
        rulesApplied = rulesApplied == null ? List.of() : List.copyOf(rulesApplied);
    }

    public static OptimizationInfo from(OptimizationResult result, ExecutionPlanNodeDto executedPlan) {
        return new OptimizationInfo(
                result.mode().name(),
                ExecutionPlanNodeDto.from(result.originalPlan().root()),
                executedPlan,
                result.rulesApplied().stream()
                        .map(trace -> new OptimizationTraceDto(trace.rule(), trace.decision(), trace.reason()))
                        .toList()
        );
    }
}
