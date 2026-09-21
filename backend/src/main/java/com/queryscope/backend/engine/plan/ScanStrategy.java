package com.queryscope.backend.engine.plan;

import com.queryscope.backend.engine.execution.QueryExecutionException;

import java.util.Locale;

public enum ScanStrategy {
    TABLE,
    INDEX;

    public static ScanStrategy from(String value) {
        if (value == null || value.isBlank()) {
            return TABLE;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("TABLE_SCAN")) {
            normalized = "TABLE";
        }
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new QueryExecutionException(
                    "Invalid scan strategy '" + value + "'. Supported strategies: TABLE, INDEX."
            );
        }
    }
}
