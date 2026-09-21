package com.queryscope.backend.dto;

public record CreateIndexRequest(String name, String table, String column) {
}
