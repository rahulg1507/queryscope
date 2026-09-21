package com.queryscope.backend.engine.execution;

import java.util.List;

public record OperatorMetrics(
        String operator,
        int inputRows,
        int outputRows,
        List<OperatorMetrics> children
) {
    public OperatorMetrics {
        children = children == null ? List.of() : List.copyOf(children);
    }
}
