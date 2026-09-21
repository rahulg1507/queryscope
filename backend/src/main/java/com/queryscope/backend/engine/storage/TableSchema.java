package com.queryscope.backend.engine.storage;

import com.queryscope.backend.engine.execution.QueryExecutionException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TableSchema {

    private final List<ColumnDefinition> columns;
    private final Map<String, ColumnDefinition> columnsByName;

    public TableSchema(List<ColumnDefinition> columns) {
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("A table schema requires at least one column");
        }
        List<ColumnDefinition> orderedColumns = new ArrayList<>();
        Map<String, ColumnDefinition> lookup = new LinkedHashMap<>();
        for (ColumnDefinition column : columns) {
            String key = normalize(column.name());
            if (lookup.putIfAbsent(key, column) != null) {
                throw new IllegalArgumentException("Duplicate column name '" + column.name() + "'.");
            }
            orderedColumns.add(column);
        }
        this.columns = List.copyOf(orderedColumns);
        this.columnsByName = Collections.unmodifiableMap(new LinkedHashMap<>(lookup));
    }

    public List<ColumnDefinition> columns() {
        return columns;
    }

    public ColumnDefinition findColumn(String name) {
        return columnsByName.get(normalize(name));
    }

    public ColumnDefinition requireColumn(String name, String tableName) {
        ColumnDefinition column = findColumn(name);
        if (column == null) {
            throw new QueryExecutionException("Unknown column '" + name + "' in table '" + tableName + "'.");
        }
        return column;
    }

    public List<ColumnDefinition> selectColumns(List<String> names, String tableName) {
        List<ColumnDefinition> selected = new ArrayList<>();
        for (String name : names) {
            selected.add(requireColumn(name, tableName));
        }
        return List.copyOf(selected);
    }

    public Map<String, Object> validateAndOrder(Map<String, ?> values) {
        if (values == null) {
            throw new QueryExecutionException("Row values must not be null.");
        }
        Map<String, Object> supplied = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            String key = normalize(entry.getKey());
            if (supplied.putIfAbsent(key, entry.getValue()) != null) {
                throw new QueryExecutionException("Duplicate row value for column '" + entry.getKey() + "'.");
            }
            if (!columnsByName.containsKey(key)) {
                throw new QueryExecutionException("Unknown column '" + entry.getKey() + "' in row.");
            }
        }
        if (supplied.size() != columns.size()) {
            for (ColumnDefinition column : columns) {
                if (!supplied.containsKey(normalize(column.name()))) {
                    throw new QueryExecutionException("Missing required column '" + column.name() + "' in row.");
                }
            }
        }

        Map<String, Object> ordered = new LinkedHashMap<>();
        for (ColumnDefinition column : columns) {
            ordered.put(column.name(), column.type().coerce(supplied.get(normalize(column.name())), column.name()));
        }
        return ordered;
    }

    public TableSchema project(List<String> names, String tableName) {
        return new TableSchema(selectColumns(names, tableName));
    }

    private static String normalize(String name) {
        if (name == null || name.isBlank()) {
            throw new QueryExecutionException("Column name must not be blank.");
        }
        return name.toLowerCase(Locale.ROOT);
    }
}
