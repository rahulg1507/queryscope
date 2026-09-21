package com.queryscope.backend.dto;

public record OptimizationTraceDto(
        String rule,
        String decision,
        String reason
) {
}
