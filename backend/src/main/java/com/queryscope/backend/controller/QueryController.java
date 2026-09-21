package com.queryscope.backend.controller;

import com.queryscope.backend.dto.ParseQueryRequest;
import com.queryscope.backend.dto.ExecutionSettings;
import com.queryscope.backend.engine.ast.SelectStatement;
import com.queryscope.backend.engine.execution.QueryResult;
import com.queryscope.backend.engine.plan.JoinStrategy;
import com.queryscope.backend.engine.plan.ScanStrategy;
import com.queryscope.backend.engine.plan.ExecutionMode;
import com.queryscope.backend.service.QueryExecutionService;
import com.queryscope.backend.service.QueryParserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    private final QueryParserService queryParserService;
    private final QueryExecutionService queryExecutionService;

    public QueryController(QueryParserService queryParserService, QueryExecutionService queryExecutionService) {
        this.queryParserService = queryParserService;
        this.queryExecutionService = queryExecutionService;
    }

    @PostMapping("/parse")
    public SelectStatement parse(@RequestBody ParseQueryRequest request) {
        return queryParserService.parse(request == null ? null : request.sql());
    }

    @PostMapping("/execute")
    public QueryResult execute(@RequestBody ParseQueryRequest request) {
        ExecutionSettings settings = request == null ? null : request.effectiveExecution();
        return queryExecutionService.execute(
                request == null ? null : request.sql(),
                ExecutionMode.from(settings == null ? null : settings.mode()),
                JoinStrategy.from(settings == null ? null : settings.joinStrategy()),
                ScanStrategy.from(settings == null ? null : settings.scanStrategy())
        );
    }
}
