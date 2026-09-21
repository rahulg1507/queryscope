package com.queryscope.backend.dto;

public record ParseQueryRequest(String sql, String joinStrategy) {

    public ParseQueryRequest(String sql) {
        this(sql, null);
    }
}
