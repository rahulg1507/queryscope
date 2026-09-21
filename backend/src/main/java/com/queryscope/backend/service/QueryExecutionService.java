package com.queryscope.backend.service;

import com.queryscope.backend.engine.ast.SelectStatement;
import com.queryscope.backend.engine.execution.QueryExecutor;
import com.queryscope.backend.engine.execution.QueryResult;
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
        SelectStatement statement = queryParserService.parse(sql);
        return queryExecutor.execute(statement);
    }
}
