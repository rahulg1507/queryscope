package com.queryscope.backend.controller;

import com.queryscope.backend.dto.ParseQueryRequest;
import com.queryscope.backend.engine.ast.SelectStatement;
import com.queryscope.backend.service.QueryParserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    private final QueryParserService queryParserService;

    public QueryController(QueryParserService queryParserService) {
        this.queryParserService = queryParserService;
    }

    @PostMapping("/parse")
    public SelectStatement parse(@RequestBody ParseQueryRequest request) {
        return queryParserService.parse(request == null ? null : request.sql());
    }
}
