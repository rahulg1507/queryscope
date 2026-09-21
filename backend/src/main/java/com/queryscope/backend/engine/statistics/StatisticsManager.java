package com.queryscope.backend.engine.statistics;

import com.queryscope.backend.engine.index.TableIndex;
import com.queryscope.backend.engine.storage.ColumnDefinition;
import com.queryscope.backend.engine.storage.DataType;
import com.queryscope.backend.engine.storage.Database;
import com.queryscope.backend.engine.storage.Row;
import com.queryscope.backend.engine.storage.Table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

/** Builds one consistent statistics snapshot per planning request. */
public final class StatisticsManager {

    private final Database database;

    public StatisticsManager(Database database) {
        if (database == null) {
            throw new IllegalArgumentException("Statistics require a database.");
        }
        this.database = database;
    }

    public StatisticsSnapshot snapshot() {
        Map<String, TableStatistics> tables = new LinkedHashMap<>();
        for (String tableName : database.tableNames()) {
            Table table = database.requireTable(tableName);
            tables.put(table.name().toLowerCase(Locale.ROOT), collect(table));
        }
        return new StatisticsSnapshot(tables);
    }

    private TableStatistics collect(Table table) {
        List<Row> rows = table.rows();
        Map<String, ColumnStatistics> columns = new LinkedHashMap<>();
        for (ColumnDefinition column : table.schema().columns()) {
            Set<Object> distinct = new LinkedHashSet<>();
            List<Object> values = new ArrayList<>();
            for (Row row : rows) {
                Object value = row.get(column.name());
                distinct.add(value);
                values.add(value);
            }
            Object min = null;
            Object max = null;
            if (isNumeric(column.type()) && !values.isEmpty()) {
                Comparator<Object> comparator = numericComparator(column.type());
                min = values.stream().min(comparator).orElse(null);
                max = values.stream().max(comparator).orElse(null);
            }
            columns.put(column.name().toLowerCase(), new ColumnStatistics(
                    column.name(), column.type(), distinct.size(), min, max
            ));
        }
        List<IndexStatistics> indexes = table.indexes().stream()
                .map(index -> new IndexStatistics(index.name(), index.columnName(),
                        index.distinctKeyCount(), index.entryCount()))
                .toList();
        return new TableStatistics(table.name(), rows.size(), columns, indexes);
    }

    private static boolean isNumeric(DataType type) {
        return type == DataType.INTEGER || type == DataType.DOUBLE;
    }

    private static Comparator<Object> numericComparator(DataType type) {
        return type == DataType.INTEGER
                ? (left, right) -> Long.compare(((Number) left).longValue(), ((Number) right).longValue())
                : (left, right) -> Double.compare(((Number) left).doubleValue(), ((Number) right).doubleValue());
    }
}
