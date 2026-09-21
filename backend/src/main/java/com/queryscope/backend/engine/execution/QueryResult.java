package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.storage.Row;
import com.queryscope.backend.engine.storage.TableSchema;
import com.queryscope.backend.engine.plan.ExecutionPlanNode;
import com.queryscope.backend.engine.plan.ExecutionPlanNodeDto;

import java.util.ArrayList;
import java.util.List;

public record QueryResult(
        List<ResultColumn> columns,
        List<List<Object>> rows,
        int rowCount,
        ExecutionMetrics metrics,
        ExecutionPlanNodeDto executionPlan
) {
    public QueryResult(List<ResultColumn> columns, List<List<Object>> rows, int rowCount, ExecutionMetrics metrics) {
        this(columns, rows, rowCount, metrics, null);
    }

    public QueryResult {
        columns = List.copyOf(columns);
        List<List<Object>> copiedRows = new ArrayList<>();
        for (List<Object> row : rows) {
            copiedRows.add(List.copyOf(row));
        }
        rows = List.copyOf(copiedRows);
    }

    public static QueryResult from(OperatorResult result) {
        return from(result, null);
    }

    public static QueryResult from(OperatorResult result, ExecutionPlanNode plan) {
        TableSchema schema = result.schema();
        List<ResultColumn> columns = schema.columns().stream()
                .map(column -> new ResultColumn(column.name(), column.type()))
                .toList();
        List<List<Object>> rows = result.rows().stream()
                .map(Row::values)
                .map(values -> schema.columns().stream().map(column -> values.get(column.name())).toList())
                .toList();
        int rowsScanned = findTableScanInput(result.metrics());
        return new QueryResult(columns, rows, rows.size(), new ExecutionMetrics(rowsScanned, rows.size()),
                plan == null ? null : ExecutionPlanNodeDto.from(plan));
    }

    private static int findTableScanInput(OperatorMetrics metrics) {
        if ("TableScan".equals(metrics.operator())) {
            return metrics.inputRows();
        }
        return metrics.children().stream().mapToInt(QueryResult::findTableScanInput).max().orElse(metrics.inputRows());
    }
}
