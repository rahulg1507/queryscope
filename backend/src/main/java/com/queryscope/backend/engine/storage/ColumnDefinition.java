package com.queryscope.backend.engine.storage;

public record ColumnDefinition(String name, DataType type) {

    public ColumnDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Column name must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Column type must not be null");
        }
    }
}
