package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.ast.SelectStatement;
import com.queryscope.backend.engine.plan.ExecutionPlan;
import com.queryscope.backend.engine.plan.ExecutionPlanBuilder;
import com.queryscope.backend.engine.plan.ExecutionPlanNode;
import com.queryscope.backend.engine.plan.ExecutionMode;
import com.queryscope.backend.engine.plan.JoinStrategy;
import com.queryscope.backend.engine.plan.ScanStrategy;
import com.queryscope.backend.engine.storage.Database;
import com.queryscope.backend.engine.optimizer.OptimizationResult;
import com.queryscope.backend.engine.optimizer.QueryOptimizer;

public final class InMemoryQueryExecutor implements QueryExecutor {

    private final ExecutionPlanBuilder planBuilder;
    private final ExecutionPlanExecutor planExecutor;
    private final QueryOptimizer optimizer;

    public InMemoryQueryExecutor(Database database) {
        this(database, new ExecutionPlanBuilder(), new ExecutionPlanExecutor(database));
    }

    public InMemoryQueryExecutor(ExecutionPlanBuilder planBuilder, ExecutionPlanExecutor planExecutor) {
        this(null, planBuilder, planExecutor);
    }

    public InMemoryQueryExecutor(Database database, ExecutionPlanBuilder planBuilder, ExecutionPlanExecutor planExecutor) {
        if (planBuilder == null || planExecutor == null) {
            throw new IllegalArgumentException("An in-memory executor requires a plan builder and executor.");
        }
        this.planBuilder = planBuilder;
        this.planExecutor = planExecutor;
        this.optimizer = database == null ? null : new QueryOptimizer(database);
    }

    @Override
    public QueryResult execute(SelectStatement statement) {
        return execute(statement, JoinStrategy.NESTED_LOOP);
    }

    @Override
    public QueryResult execute(SelectStatement statement, JoinStrategy joinStrategy) {
        return execute(statement, joinStrategy, ScanStrategy.TABLE);
    }

    @Override
    public QueryResult execute(SelectStatement statement, JoinStrategy joinStrategy, ScanStrategy scanStrategy) {
        ExecutionPlan plan = planBuilder.build(statement, joinStrategy, scanStrategy);
        OperatorResult result = planExecutor.execute(plan);
        ExecutionPlanNode executedPlan = ExecutionPlanMetadata.attach(plan.root(), result.metrics());
        return QueryResult.from(result, executedPlan);
    }

    @Override
    public QueryResult execute(
            SelectStatement statement,
            ExecutionMode mode,
            JoinStrategy joinStrategy,
            ScanStrategy scanStrategy
    ) {
        if (optimizer == null) {
            throw new QueryExecutionException("Rule-based optimization requires a database-backed executor.");
        }
        ExecutionPlan initialPlan = mode == ExecutionMode.AUTO
                ? planBuilder.build(statement, JoinStrategy.NESTED_LOOP, ScanStrategy.TABLE)
                : planBuilder.build(statement, joinStrategy, scanStrategy);
        OptimizationResult optimization = optimizer.optimize(initialPlan, mode);
        OperatorResult result = planExecutor.execute(optimization.optimizedPlan());
        ExecutionPlanNode executedPlan = ExecutionPlanMetadata.attach(optimization.optimizedPlan().root(), result.metrics());
        return QueryResult.from(result, executedPlan, optimization);
    }
}
