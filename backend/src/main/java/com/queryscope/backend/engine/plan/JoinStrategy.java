package com.queryscope.backend.engine.plan;

import com.queryscope.backend.engine.execution.QueryExecutionException;

import java.util.Locale;

public enum JoinStrategy {
    NESTED_LOOP("NESTED_LOOP_JOIN"),
    HASH("HASH_JOIN");

    private final String planType;

    JoinStrategy(String planType) {
        this.planType = planType;
    }

    public String planType() {
        return planType;
    }

    public static JoinStrategy from(String value) {
        if (value == null || value.isBlank()) {
            return NESTED_LOOP;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new QueryExecutionException(
                    "Invalid join strategy '" + value + "'. Supported strategies: NESTED_LOOP, HASH."
            );
        }
    }
}
