package com.queryscope.backend.engine.ast;

public record AggregateWildcardArgument() implements AggregateArgument {

    @Override
    public String display() {
        return "*";
    }
}
