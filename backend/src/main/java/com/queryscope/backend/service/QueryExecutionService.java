package com.queryscope.backend.service;

import com.queryscope.backend.engine.ast.SelectStatement;
import com.queryscope.backend.engine.execution.QueryExecutor;
import com.queryscope.backend.engine.execution.QueryResult;
import com.queryscope.backend.engine.plan.JoinStrategy;
import com.queryscope.backend.engine.plan.ScanStrategy;
import com.queryscope.backend.engine.plan.ExecutionMode;
import org.springframework.stereotype.Service;

@Service
public class QueryExecutionService {

    private final QueryParserService queryParserService;
    private final QueryExecutor queryExecutor;

    public QueryExecutionService(QueryParserService queryParserService, QueryExecutor queryExecutor) {
        this.queryParserService = queryParserService;
        this.queryExecutor = queryExecutor;
    }

    public QueryResult execute(String sql) {
        return execute(sql, ExecutionMode.AUTO, JoinStrategy.NESTED_LOOP, ScanStrategy.TABLE);
    }

    public QueryResult execute(String sql, JoinStrategy joinStrategy) {
        return execute(sql, ExecutionMode.MANUAL, joinStrategy, ScanStrategy.TABLE);
    }

    public QueryResult execute(String sql, JoinStrategy joinStrategy, ScanStrategy scanStrategy) {
        return execute(sql, ExecutionMode.MANUAL, joinStrategy, scanStrategy);
    }

    public QueryResult execute(
            String sql,
            ExecutionMode mode,
            JoinStrategy joinStrategy,
            ScanStrategy scanStrategy
    ) {
        SelectStatement statement = queryParserService.parse(sql);
        return queryExecutor.execute(statement, mode, joinStrategy, scanStrategy);
    }
}
