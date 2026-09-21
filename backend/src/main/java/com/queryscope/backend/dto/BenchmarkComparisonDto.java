package com.queryscope.backend.dto;

import java.util.Map;

public record BenchmarkComparisonDto(
        String strategy,
        String planType,
        int actualRowsReturned,
        Map<String, Object> actualMetrics
) {
}
