package com.queryscope.backend.dto;

import java.util.List;
import java.util.Map;

public record BenchmarkOptimizerDto(
        String selectedPlan,
        Double estimatedRows,
        Double estimatedCost,
        String selectionReason,
        List<OptimizerCandidateDto> candidates,
        BenchmarkEstimationErrorDto estimationError,
        int actualRows,
        Map<String, Object> actualMetrics
) {
}
