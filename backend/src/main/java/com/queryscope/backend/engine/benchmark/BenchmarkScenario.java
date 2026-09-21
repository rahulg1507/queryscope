package com.queryscope.backend.engine.benchmark;

import com.queryscope.backend.engine.execution.QueryExecutionException;

import java.util.Locale;

public enum BenchmarkScenario {
    INDEX_EQUALITY,
    INDEX_RANGE_SELECTIVE,
    INDEX_RANGE_BROAD,
    JOIN_SMALL,
    JOIN_MEDIUM,
    JOIN_LARGE,
    OPTIMIZER_SCAN,
    OPTIMIZER_JOIN;

    public boolean scanScenario() {
        return name().startsWith("INDEX_") || this == OPTIMIZER_SCAN;
    }

    public boolean joinScenario() {
        return name().startsWith("JOIN_") || this == OPTIMIZER_JOIN;
    }

    public static BenchmarkScenario from(String value) {
        if (value == null || value.isBlank()) {
            throw invalid(value);
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw invalid(value);
        }
    }

    private static QueryExecutionException invalid(String value) {
        return new QueryExecutionException(
                "Invalid benchmark scenario '" + value + "'. Supported scenarios: "
                        + String.join(", ", names()) + "."
        );
    }

    private static String[] names() {
        return java.util.Arrays.stream(values()).map(Enum::name).toArray(String[]::new);
    }
}
