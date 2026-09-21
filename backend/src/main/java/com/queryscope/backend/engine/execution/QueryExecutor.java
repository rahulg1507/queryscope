package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.ast.SelectStatement;

public interface QueryExecutor {

    QueryResult execute(SelectStatement statement);
}
