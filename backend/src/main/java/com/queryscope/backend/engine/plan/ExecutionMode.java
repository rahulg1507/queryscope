package com.queryscope.backend.engine.plan;

import com.queryscope.backend.engine.execution.QueryExecutionException;

import java.util.Locale;

public enum ExecutionMode {
    AUTO,
    MANUAL;

    public static ExecutionMode from(String value) {
        if (value == null || value.isBlank()) {
            return AUTO;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new QueryExecutionException(
                    "Invalid execution mode '" + value + "'. Supported modes: AUTO, MANUAL."
            );
        }
    }
}
