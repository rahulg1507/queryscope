package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.storage.ColumnDefinition;
import com.queryscope.backend.engine.storage.ColumnResolution;
import com.queryscope.backend.engine.storage.Row;
import com.queryscope.backend.engine.storage.TableSchema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProjectionOperator implements QueryOperator {

    private final QueryOperator input;
    private final List<String> columnNames;
    private final String tableName;

    public ProjectionOperator(QueryOperator input, List<String> columnNames, String tableName) {
        this.input = input;
        this.columnNames = List.copyOf(columnNames);
        this.tableName = tableName;
    }

    @Override
    public OperatorResult execute() {
        OperatorResult inputResult = input.execute();
        List<ColumnResolution> resolvedColumns = columnNames.stream()
                .map(column -> inputResult.schema().resolveColumn(column, tableName))
                .toList();
        TableSchema outputSchema = new TableSchema(resolvedColumns.stream()
                .map(resolved -> new ColumnDefinition(resolved.outputName(), resolved.column().type()))
                .toList());
        List<Row> projected = new ArrayList<>();
        for (Row row : inputResult.rows()) {
            Map<String, Object> values = new LinkedHashMap<>();
            for (int index = 0; index < outputSchema.columns().size(); index++) {
                ColumnDefinition column = outputSchema.columns().get(index);
                values.put(column.name(), row.get(resolvedColumns.get(index).actualName()));
            }
            projected.add(new Row(outputSchema, values));
        }
        return new OperatorResult(
                outputSchema,
                projected,
                new OperatorMetrics("Projection", inputResult.rows().size(), projected.size(), List.of(inputResult.metrics()))
        );
    }
}
