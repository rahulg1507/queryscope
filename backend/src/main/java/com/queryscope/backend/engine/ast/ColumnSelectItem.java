package com.queryscope.backend.engine.ast;

public record ColumnSelectItem(String name) implements SelectItem {

    public ColumnSelectItem {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Column name must not be blank");
        }
    }

    @Override
    public String getType() {
        return "COLUMN";
    }
}
