package com.queryscope.backend.controller;

import com.queryscope.backend.dto.CreateIndexRequest;
import com.queryscope.backend.dto.SchemaColumnDto;
import com.queryscope.backend.dto.SchemaIndexDto;
import com.queryscope.backend.dto.SchemaResponse;
import com.queryscope.backend.dto.SchemaTableDto;
import com.queryscope.backend.engine.storage.Database;
import com.queryscope.backend.engine.storage.Table;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schema")
public class SchemaController {

    private final Database database;

    public SchemaController(Database database) {
        this.database = database;
    }

    @GetMapping
    public SchemaResponse schema() {
        return schemaResponse();
    }

    @PostMapping("/indexes")
    public SchemaResponse createIndex(@RequestBody CreateIndexRequest request) {
        if (request == null) {
            throw new com.queryscope.backend.engine.execution.QueryExecutionException("Index request must not be null.");
        }
        database.createIndex(request.name(), request.table(), request.column());
        return schemaResponse();
    }

    private SchemaResponse schemaResponse() {
        return new SchemaResponse(database.tableNames().stream()
                .map(database::requireTable)
                .map(this::toDto)
                .toList());
    }

    private SchemaTableDto toDto(Table table) {
        return new SchemaTableDto(
                table.name(),
                table.schema().columns().stream().map(column -> new SchemaColumnDto(column.name(), column.type())).toList(),
                table.indexes().stream().map(index -> new SchemaIndexDto(index.name(), index.columnName())).toList()
        );
    }
}
