package com.queryscope.backend.engine.ast;

public record StringLiteral(String value) implements Expression {

    public StringLiteral {
        if (value == null) {
            throw new IllegalArgumentException("String literal value must not be null");
        }
    }

    @Override
    public String getType() {
        return "STRING";
    }
}
