package com.queryscope.backend.engine.storage;

import com.queryscope.backend.engine.execution.QueryExecutionException;
import com.queryscope.backend.engine.index.TableIndex;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;

public final class Table {

    private final String name;
    private final TableSchema schema;
    private final List<Row> rows = new ArrayList<>();
    private final Map<String, TableIndex> indexes = new LinkedHashMap<>();

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
        Row row = new Row(schema, values);
        int position = rows.size();
        rows.add(row);
        indexes.values().forEach(index -> index.insert(row.get(index.columnName()), position));
    }

    public synchronized List<Row> rows() {
        return List.copyOf(rows);
    }

    public synchronized TableIndex createIndex(String indexName, String columnName) {
        String key = normalize(indexName);
        if (indexes.containsKey(key)) {
            throw new QueryExecutionException("Index '" + indexName + "' already exists on table '" + name + "'.");
        }
        ColumnDefinition column = schema.findColumn(columnName);
        if (column == null) {
            throw new QueryExecutionException("Unknown column '" + columnName + "' in table '" + name + "'.");
        }
        TableIndex index = new TableIndex(indexName, name, column.name(), column.type());
        for (int position = 0; position < rows.size(); position++) {
            index.insert(rows.get(position).get(column.name()), position);
        }
        indexes.put(key, index);
        return index;
    }

    public synchronized TableIndex indexForColumn(String columnName) {
        String simple = simpleName(columnName);
        return indexes.values().stream()
                .filter(index -> index.columnName().equalsIgnoreCase(simple))
                .findFirst()
                .orElse(null);
    }

    public synchronized List<TableIndex> indexes() {
        return Collections.unmodifiableList(new ArrayList<>(indexes.values()));
    }

    public boolean hasName(String candidate) {
        return name.toLowerCase(Locale.ROOT).equals(candidate.toLowerCase(Locale.ROOT));
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new QueryExecutionException("Index name must not be blank.");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static String simpleName(String value) {
        int separator = value.lastIndexOf('.');
        return separator < 0 ? value : value.substring(separator + 1);
    }
}
