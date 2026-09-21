package com.queryscope.backend.engine.ast;

public record NumberLiteral(long value) implements Expression {

    @Override
    public String getType() {
        return "NUMBER";
    }
}
