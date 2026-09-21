package com.queryscope.backend.engine.benchmark;

import com.queryscope.backend.dto.BenchmarkEstimationErrorDto;

public final class EstimationErrorCalculator {

    private EstimationErrorCalculator() {
    }

    public static BenchmarkEstimationErrorDto calculate(double estimatedRows, int actualRows) {
        double absolute = Math.abs(estimatedRows - actualRows);
        Double percentage = actualRows == 0 ? null : absolute / actualRows * 100.0;
        return new BenchmarkEstimationErrorDto(estimatedRows, actualRows, absolute, percentage);
    }
}
