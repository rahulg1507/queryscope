package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.ast.SelectStatement;
import com.queryscope.backend.engine.plan.JoinStrategy;

public interface QueryExecutor {

    QueryResult execute(SelectStatement statement);

    default QueryResult execute(SelectStatement statement, JoinStrategy joinStrategy) {
        return execute(statement);
    }
}
