package com.queryscope.backend.engine.ast;

public record AggregateExpression(
        AggregateFunction function,
        AggregateArgument argument
) {
    public AggregateExpression {
        if (function == null || argument == null) {
            throw new IllegalArgumentException("An aggregate expression requires a function and argument.");
        }
        if (argument instanceof AggregateWildcardArgument && function != AggregateFunction.COUNT) {
            throw new IllegalArgumentException(function + " does not support '*'.");
        }
    }

    public String display() {
        return function.name() + "(" + argument.display() + ")";
    }
}
