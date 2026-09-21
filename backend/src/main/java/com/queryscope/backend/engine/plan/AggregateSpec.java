package com.queryscope.backend.engine.plan;

import com.queryscope.backend.engine.ast.AggregateFunction;

public record AggregateSpec(
        AggregateFunction function,
        String column
) {
    public AggregateSpec {
        if (function == null) {
            throw new IllegalArgumentException("Aggregate function must not be null.");
        }
        if (function != AggregateFunction.COUNT && (column == null || column.isBlank())) {
            throw new IllegalArgumentException(function + " requires a column.");
        }
    }

    public boolean isCountStar() {
        return function == AggregateFunction.COUNT && column == null;
    }

    public String display() {
        return function.name() + "(" + (column == null ? "*" : column) + ")";
    }
}
