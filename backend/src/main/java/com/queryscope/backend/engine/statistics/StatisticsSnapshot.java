package com.queryscope.backend.engine.statistics;

import java.util.Map;
import java.util.Locale;

public record StatisticsSnapshot(Map<String, TableStatistics> tables) {
    public StatisticsSnapshot {
        tables = Map.copyOf(tables);
    }

    public TableStatistics table(String tableName) {
        TableStatistics table = tables.get(tableName.toLowerCase(Locale.ROOT));
        if (table == null) {
            throw new IllegalArgumentException("No statistics exist for table '" + tableName + "'.");
        }
        return table;
    }
}
