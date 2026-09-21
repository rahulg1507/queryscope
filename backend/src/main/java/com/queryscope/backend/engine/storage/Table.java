package com.queryscope.backend.engine.storage;

import com.queryscope.backend.engine.execution.QueryExecutionException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Table {

    private final String name;
    private final TableSchema schema;
    private final List<Row> rows = new ArrayList<>();

    public Table(String name, TableSchema schema) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Table name must not be blank");
        }
        this.name = name;
        this.schema = schema;
    }

    public String name() {
        return name;
    }

    public TableSchema schema() {
        return schema;
    }

    public synchronized void insert(Map<String, ?> values) {
        rows.add(new Row(schema, values));
    }

    public synchronized List<Row> rows() {
        return List.copyOf(rows);
    }

    public boolean hasName(String candidate) {
        return name.toLowerCase(Locale.ROOT).equals(candidate.toLowerCase(Locale.ROOT));
    }
}
