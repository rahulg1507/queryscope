package com.queryscope.backend.engine.statistics;

import java.util.List;
import java.util.Map;

public record TableStatistics(
        String name,
        long rowCount,
        Map<String, ColumnStatistics> columns,
        List<IndexStatistics> indexes
) {
    public TableStatistics {
        columns = Map.copyOf(columns);
        indexes = List.copyOf(indexes);
    }

    public ColumnStatistics column(String columnName) {
        return columns.values().stream()
                .filter(column -> column.name().equalsIgnoreCase(columnName)
                        || column.name().equalsIgnoreCase(simpleName(columnName)))
                .findFirst()
                .orElse(null);
    }

    private static String simpleName(String name) {
        int separator = name.lastIndexOf('.');
        return separator < 0 ? name : name.substring(separator + 1);
    }
}
