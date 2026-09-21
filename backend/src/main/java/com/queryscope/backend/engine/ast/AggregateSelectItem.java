package com.queryscope.backend.engine.ast;

public record AggregateSelectItem(AggregateExpression expression) implements SelectItem {

    public AggregateSelectItem {
        if (expression == null) {
            throw new IllegalArgumentException("Aggregate select item requires an expression.");
        }
    }

    @Override
    public String getType() {
        return "AGGREGATE";
    }
}
