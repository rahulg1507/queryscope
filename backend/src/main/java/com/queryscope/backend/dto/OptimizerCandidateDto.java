package com.queryscope.backend.dto;

public record OptimizerCandidateDto(
        String planType,
        double estimatedRows,
        double estimatedCost
) {
}
