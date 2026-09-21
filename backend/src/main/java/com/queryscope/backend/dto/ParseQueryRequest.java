package com.queryscope.backend.dto;

public record ParseQueryRequest(
        String sql,
        String joinStrategy,
        String scanStrategy,
        String mode,
        ExecutionSettings execution
) {

    public ParseQueryRequest(String sql) {
        this(sql, null, null, null, null);
    }

    public ParseQueryRequest(String sql, String joinStrategy) {
        this(sql, joinStrategy, null, null, null);
    }

    public ParseQueryRequest(String sql, String joinStrategy, String scanStrategy) {
        this(sql, joinStrategy, scanStrategy, null, null);
    }

    public ExecutionSettings effectiveExecution() {
        return execution != null ? execution : new ExecutionSettings(mode, joinStrategy, scanStrategy);
    }
}
