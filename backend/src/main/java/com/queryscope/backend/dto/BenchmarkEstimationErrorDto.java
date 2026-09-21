package com.queryscope.backend.dto;

public record BenchmarkEstimationErrorDto(
        double estimatedRows,
        int actualRows,
        double absoluteError,
        Double percentageError
) {
}
