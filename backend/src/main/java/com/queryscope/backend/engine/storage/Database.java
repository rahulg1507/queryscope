package com.queryscope.backend.engine.storage;

import com.queryscope.backend.engine.execution.QueryExecutionException;
import com.queryscope.backend.engine.index.TableIndex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Database {

    private final Map<String, Table> tables = new LinkedHashMap<>();

    public synchronized void createTable(Table table) {
        String key = normalize(table.name());
        if (tables.containsKey(key)) {
            throw new QueryExecutionException("Table '" + table.name() + "' already exists.");
        }
        tables.put(key, table);
    }

    public synchronized Table getTable(String name) {
        return tables.get(normalize(name));
    }

    public synchronized Table requireTable(String name) {
        Table table = getTable(name);
        if (table == null) {
            throw new QueryExecutionException("Unknown table '" + name + "'.");
        }
        return table;
    }

    public synchronized boolean hasTable(String name) {
        return tables.containsKey(normalize(name));
    }

    public synchronized List<String> tableNames() {
        return Collections.unmodifiableList(new ArrayList<>(tables.values().stream().map(Table::name).toList()));
    }

    public synchronized TableIndex createIndex(String indexName, String tableName, String columnName) {
        String normalizedIndex = normalize(indexName);
        for (Table existing : tables.values()) {
            if (existing.indexes().stream().anyMatch(index -> index.name().equalsIgnoreCase(normalizedIndex))) {
                throw new QueryExecutionException("Index '" + indexName + "' already exists.");
            }
        }
        return requireTable(tableName).createIndex(indexName, columnName);
    }

    public synchronized List<TableIndex> indexes() {
        return tables.values().stream().flatMap(table -> table.indexes().stream()).toList();
    }

    private static String normalize(String name) {
        if (name == null || name.isBlank()) {
            throw new QueryExecutionException("Table name must not be blank.");
        }
        return name.toLowerCase(Locale.ROOT);
    }
}
