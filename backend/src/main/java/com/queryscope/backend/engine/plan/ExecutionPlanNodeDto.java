package com.queryscope.backend.engine.plan;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

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
        return new ExecutionPlanNodeDto(
                node.type(),
                node.details(),
                node.inputRows(),
                node.outputRows(),
                node.children().stream().map(ExecutionPlanNodeDto::from).toList()
        );
    }
}
