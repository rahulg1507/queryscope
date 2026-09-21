package com.queryscope.backend.engine.ast;

public record BooleanLiteral(boolean value) implements Expression {

    @Override
    public String getType() {
        return "BOOLEAN";
    }
}
