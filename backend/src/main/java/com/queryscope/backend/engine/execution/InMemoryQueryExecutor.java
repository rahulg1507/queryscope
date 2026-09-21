package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.ast.SelectStatement;
import com.queryscope.backend.engine.plan.ExecutionPlan;
import com.queryscope.backend.engine.plan.ExecutionPlanBuilder;
import com.queryscope.backend.engine.plan.ExecutionPlanNode;
import com.queryscope.backend.engine.plan.JoinStrategy;
import com.queryscope.backend.engine.storage.Database;

public final class InMemoryQueryExecutor implements QueryExecutor {

    private final ExecutionPlanBuilder planBuilder;
    private final ExecutionPlanExecutor planExecutor;

    public InMemoryQueryExecutor(Database database) {
        this(new ExecutionPlanBuilder(), new ExecutionPlanExecutor(database));
    }

    public InMemoryQueryExecutor(ExecutionPlanBuilder planBuilder, ExecutionPlanExecutor planExecutor) {
        this.planBuilder = planBuilder;
        this.planExecutor = planExecutor;
    }

    @Override
    public QueryResult execute(SelectStatement statement) {
        return execute(statement, JoinStrategy.NESTED_LOOP);
    }

    @Override
    public QueryResult execute(SelectStatement statement, JoinStrategy joinStrategy) {
        ExecutionPlan plan = planBuilder.build(statement, joinStrategy);
        OperatorResult result = planExecutor.execute(plan);
        ExecutionPlanNode executedPlan = ExecutionPlanMetadata.attach(plan.root(), result.metrics());
        return QueryResult.from(result, executedPlan);
    }
}
