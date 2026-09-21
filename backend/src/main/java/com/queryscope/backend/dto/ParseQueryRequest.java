package com.queryscope.backend.dto;

public record ParseQueryRequest(String sql, String joinStrategy, String scanStrategy) {

    public ParseQueryRequest(String sql) {
        this(sql, null, null);
    }

    public ParseQueryRequest(String sql, String joinStrategy) {
        this(sql, joinStrategy, null);
    }
}
