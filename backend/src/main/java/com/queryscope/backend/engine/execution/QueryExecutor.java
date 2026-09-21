package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.ast.SelectStatement;
import com.queryscope.backend.engine.plan.JoinStrategy;
import com.queryscope.backend.engine.plan.ScanStrategy;
import com.queryscope.backend.engine.plan.ExecutionMode;

public interface QueryExecutor {

    QueryResult execute(SelectStatement statement);

    default QueryResult execute(SelectStatement statement, JoinStrategy joinStrategy) {
        return execute(statement, joinStrategy, ScanStrategy.TABLE);
    }

    default QueryResult execute(SelectStatement statement, JoinStrategy joinStrategy, ScanStrategy scanStrategy) {
        return execute(statement);
    }

    default QueryResult execute(
            SelectStatement statement,
            ExecutionMode mode,
            JoinStrategy joinStrategy,
            ScanStrategy scanStrategy
    ) {
        return execute(statement, joinStrategy, scanStrategy);
    }
}
