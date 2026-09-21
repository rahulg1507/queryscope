package com.queryscope.backend.engine.execution;

import java.util.List;
import java.util.Map;

public record OperatorMetrics(
        String operator,
        int inputRows,
        int outputRows,
        List<OperatorMetrics> children,
        Map<String, Object> details
) {
    public OperatorMetrics(String operator, int inputRows, int outputRows, List<OperatorMetrics> children) {
        this(operator, inputRows, outputRows, children, Map.of());
    }

    public OperatorMetrics {
        children = children == null ? List.of() : List.copyOf(children);
        details = details == null ? Map.of() : Map.copyOf(details);
    }
}
