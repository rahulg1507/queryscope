package com.queryscope.backend.engine.benchmark;

import com.queryscope.backend.engine.execution.QueryExecutionException;

import java.util.Locale;

/** Practical deterministic sizes; LARGE is capped to keep nested-loop runs usable locally. */
public enum BenchmarkDatasetSize {
    SMALL(100, 1_000),
    MEDIUM(500, 5_000),
    LARGE(1_000, 10_000);

    private final int userRows;
    private final int expenseRows;

    BenchmarkDatasetSize(int userRows, int expenseRows) {
        this.userRows = userRows;
        this.expenseRows = expenseRows;
    }

    public int userRows() {
        return userRows;
    }

    public int expenseRows() {
        return expenseRows;
    }

    public static BenchmarkDatasetSize from(String value) {
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
                "Invalid benchmark dataset size '" + value + "'. Supported sizes: SMALL, MEDIUM, LARGE."
        );
    }
}
