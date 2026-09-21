package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.plan.ExecutionPlanNode;

import java.util.List;
import java.util.stream.IntStream;

public final class ExecutionPlanMetadata {

    private ExecutionPlanMetadata() {
    }

    public static ExecutionPlanNode attach(ExecutionPlanNode plan, OperatorMetrics metrics) {
        if (plan.children().size() != metrics.children().size()) {
            throw new IllegalArgumentException("Plan and operator child counts must match.");
        }
        List<ExecutionPlanNode> children = IntStream.range(0, plan.children().size())
                .mapToObj(index -> attach(plan.children().get(index), metrics.children().get(index)))
                .toList();
        return plan.withMetrics(metrics.inputRows(), metrics.outputRows(), children);
    }
}
