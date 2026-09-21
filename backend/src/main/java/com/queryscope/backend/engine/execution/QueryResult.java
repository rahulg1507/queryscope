package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.storage.Row;
import com.queryscope.backend.engine.storage.TableSchema;

import java.util.ArrayList;
import java.util.List;

public record QueryResult(
        List<ResultColumn> columns,
        List<List<Object>> rows,
        int rowCount,
        ExecutionMetrics metrics
) {
    public QueryResult {
        columns = List.copyOf(columns);
        List<List<Object>> copiedRows = new ArrayList<>();
        for (List<Object> row : rows) {
            copiedRows.add(List.copyOf(row));
        }
        rows = List.copyOf(copiedRows);
    }

    public static QueryResult from(OperatorResult result) {
        TableSchema schema = result.schema();
        List<ResultColumn> columns = schema.columns().stream()
                .map(column -> new ResultColumn(column.name(), column.type()))
                .toList();
        List<List<Object>> rows = result.rows().stream()
                .map(Row::values)
                .map(values -> schema.columns().stream().map(column -> values.get(column.name())).toList())
                .toList();
        int rowsScanned = findTableScanInput(result.metrics());
        return new QueryResult(columns, rows, rows.size(), new ExecutionMetrics(rowsScanned, rows.size()));
    }

    private static int findTableScanInput(OperatorMetrics metrics) {
        if ("TableScan".equals(metrics.operator())) {
            return metrics.inputRows();
        }
        return metrics.children().stream().mapToInt(QueryResult::findTableScanInput).max().orElse(metrics.inputRows());
    }
}
