package com.queryscope.backend.engine.ast;

public record ColumnExpression(String name) implements Expression {

    public ColumnExpression {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Column name must not be blank");
        }
    }

    @Override
    public String getType() {
        return "COLUMN";
    }
}
