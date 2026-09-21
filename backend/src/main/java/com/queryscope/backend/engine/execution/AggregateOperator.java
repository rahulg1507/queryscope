package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.ast.AggregateFunction;
import com.queryscope.backend.engine.plan.AggregateSpec;
import com.queryscope.backend.engine.storage.ColumnDefinition;
import com.queryscope.backend.engine.storage.ColumnResolution;
import com.queryscope.backend.engine.storage.DataType;
import com.queryscope.backend.engine.storage.Row;
import com.queryscope.backend.engine.storage.TableSchema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AggregateOperator implements QueryOperator {

    private final QueryOperator input;
    private final List<String> groupBy;
    private final List<AggregateSpec> aggregates;
    private final String tableName;

    public AggregateOperator(QueryOperator input, List<String> groupBy, List<AggregateSpec> aggregates, String tableName) {
        this.input = input;
        this.groupBy = List.copyOf(groupBy);
        this.aggregates = List.copyOf(aggregates);
        this.tableName = tableName;
    }

    @Override
    public OperatorResult execute() {
        OperatorResult inputResult = input.execute();
        List<ColumnResolution> groupColumns = groupBy.stream()
                .map(column -> inputResult.schema().resolveColumn(column, tableName))
                .toList();
        List<ResolvedAggregate> resolvedAggregates = aggregates.stream()
                .map(aggregate -> resolveAggregate(aggregate, inputResult.schema()))
                .toList();

        if (inputResult.rows().isEmpty() && groupBy.isEmpty()
                && resolvedAggregates.stream().anyMatch(aggregate -> aggregate.spec().function() != AggregateFunction.COUNT)) {
            throw new QueryExecutionException("SUM and AVG cannot produce a value for an empty input relation.");
        }

        Map<GroupKey, List<Row>> groups = new LinkedHashMap<>();
        if (groupBy.isEmpty()) {
            groups.put(new GroupKey(List.of()), inputResult.rows());
        } else {
            for (Row row : inputResult.rows()) {
                List<Object> values = groupColumns.stream().map(column -> row.get(column.actualName())).toList();
                groups.computeIfAbsent(new GroupKey(values), ignored -> new ArrayList<>()).add(row);
            }
        }

        TableSchema outputSchema = outputSchema(groupColumns, resolvedAggregates);
        List<Row> outputRows = new ArrayList<>();
        for (Map.Entry<GroupKey, List<Row>> entry : groups.entrySet()) {
            Map<String, Object> values = new LinkedHashMap<>();
            for (int index = 0; index < groupColumns.size(); index++) {
                values.put(outputSchema.columns().get(index).name(), entry.getKey().values().get(index));
            }
            for (int index = 0; index < resolvedAggregates.size(); index++) {
                ResolvedAggregate aggregate = resolvedAggregates.get(index);
                values.put(outputSchema.columns().get(groupColumns.size() + index).name(), aggregate.value(entry.getValue()));
            }
            outputRows.add(new Row(outputSchema, values));
        }

        Map<String, Object> details = Map.of(
                "groupBy", groupBy,
                "functions", aggregates.stream().map(AggregateSpec::display).toList(),
                "groups", groups.size()
        );
        return new OperatorResult(
                outputSchema,
                outputRows,
                new OperatorMetrics("Aggregate", inputResult.rows().size(), outputRows.size(),
                        List.of(inputResult.metrics()), details)
        );
    }

    private ResolvedAggregate resolveAggregate(AggregateSpec spec, TableSchema schema) {
        if (spec.isCountStar()) {
            return new ResolvedAggregate(spec, null, DataType.INTEGER);
        }
        ColumnResolution column = schema.resolveColumn(spec.column(), tableName);
        if ((spec.function() == AggregateFunction.SUM || spec.function() == AggregateFunction.AVG)
                && column.column().type() != DataType.INTEGER) {
            throw new QueryExecutionException(spec.function() + " requires a numeric column; '"
                    + spec.column() + "' is " + column.column().type() + ".");
        }
        return new ResolvedAggregate(spec, column, spec.function() == AggregateFunction.AVG ? DataType.DOUBLE : DataType.INTEGER);
    }

    private static TableSchema outputSchema(List<ColumnResolution> groupColumns, List<ResolvedAggregate> aggregates) {
        List<ColumnDefinition> columns = new ArrayList<>();
        groupColumns.forEach(column -> columns.add(new ColumnDefinition(column.outputName(), column.column().type())));
        aggregates.forEach(aggregate -> columns.add(new ColumnDefinition(aggregate.spec().display(), aggregate.outputType())));
        return new TableSchema(columns);
    }

    private record GroupKey(List<Object> values) {
        private GroupKey {
            values = List.copyOf(values);
        }
    }

    private record ResolvedAggregate(AggregateSpec spec, ColumnResolution column, DataType outputType) {
        private Object value(List<Row> rows) {
            return switch (spec.function()) {
                case COUNT -> rows.stream()
                        .filter(row -> spec.isCountStar() || row.get(column.actualName()) != null)
                        .count();
                case SUM -> sum(rows);
                case AVG -> average(rows);
            };
        }

        private long sum(List<Row> rows) {
            long total = 0;
            try {
                for (Row row : rows) {
                    total = Math.addExact(total, ((Number) row.get(column.actualName())).longValue());
                }
            } catch (ArithmeticException exception) {
                throw new QueryExecutionException("SUM overflow for column '" + spec.column() + "'.");
            }
            return total;
        }

        private double average(List<Row> rows) {
            return sum(rows) / (double) rows.size();
        }
    }
}
