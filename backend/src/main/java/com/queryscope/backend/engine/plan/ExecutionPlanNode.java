package com.queryscope.backend.engine.plan;

import java.util.List;
import java.util.Map;

public interface ExecutionPlanNode {

    String type();

    Map<String, Object> details();

    Integer inputRows();

    Integer outputRows();

    List<ExecutionPlanNode> children();

    ExecutionPlanNode withMetrics(int inputRows, int outputRows, List<ExecutionPlanNode> children);
}
