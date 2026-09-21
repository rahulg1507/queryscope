package com.queryscope.backend.engine.plan;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.queryscope.backend.engine.execution.OperatorMetrics;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExecutionPlanNodeDto(
        String type,
        Map<String, Object> details,
        Integer inputRows,
        Integer outputRows,
        List<ExecutionPlanNodeDto> children
) {
    public ExecutionPlanNodeDto {
        details = details == null ? Map.of() : Map.copyOf(details);
        children = children == null ? List.of() : List.copyOf(children);
    }

    public static ExecutionPlanNodeDto from(ExecutionPlanNode node) {
        return from(node, null);
    }

    public static ExecutionPlanNodeDto from(ExecutionPlanNode node, OperatorMetrics metrics) {
        Map<String, Object> nodeDetails = new LinkedHashMap<>(node.details());
        if (metrics != null) {
            nodeDetails.putAll(metrics.details());
        }
        return new ExecutionPlanNodeDto(
                node.type(),
                nodeDetails,
                node.inputRows(),
                node.outputRows(),
                IntStream.range(0, node.children().size())
                        .mapToObj(index -> from(node.children().get(index), childMetrics(metrics, index)))
                        .toList()
        );
    }

    private static OperatorMetrics childMetrics(OperatorMetrics metrics, int index) {
        return metrics == null ? null : metrics.children().get(index);
    }
}
