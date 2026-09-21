package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.plan.JoinCondition;
import com.queryscope.backend.engine.storage.ColumnDefinition;
import com.queryscope.backend.engine.storage.ColumnResolution;
import com.queryscope.backend.engine.storage.DataType;
import com.queryscope.backend.engine.storage.Row;
import com.queryscope.backend.engine.storage.TableSchema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NestedLoopJoinOperator implements QueryOperator {

    private final QueryOperator left;
    private final QueryOperator right;
    private final JoinCondition condition;
    private final String leftTable;
    private final String rightTable;

    public NestedLoopJoinOperator(
            QueryOperator left,
            QueryOperator right,
            JoinCondition condition,
            String leftTable,
            String rightTable
    ) {
        this.left = left;
        this.right = right;
        this.condition = condition;
        this.leftTable = leftTable;
        this.rightTable = rightTable;
    }

    @Override
    public OperatorResult execute() {
        OperatorResult leftResult = left.execute();
        OperatorResult rightResult = right.execute();
        ColumnResolution leftColumn = leftResult.schema().resolveColumn(condition.leftColumn(), leftTable);
        ColumnResolution rightColumn = rightResult.schema().resolveColumn(condition.rightColumn(), rightTable);
        ensureCompatible(leftColumn.column().type(), rightColumn.column().type());

        TableSchema joinedSchema = joinedSchema(leftResult.schema(), rightResult.schema(), leftTable, rightTable);
        List<Row> joinedRows = new ArrayList<>();
        int comparisons = 0;
        int matches = 0;
        for (Row leftRow : leftResult.rows()) {
            for (Row rightRow : rightResult.rows()) {
                comparisons++;
                if (valuesEqual(leftRow.get(leftColumn.actualName()), rightRow.get(rightColumn.actualName()))) {
                    joinedRows.add(combine(joinedSchema, leftRow, rightRow, leftResult.schema(), rightResult.schema(), leftTable, rightTable));
                    matches++;
                }
            }
        }
        Map<String, Object> details = Map.of(
                "leftRows", leftResult.rows().size(),
                "rightRows", rightResult.rows().size(),
                "comparisons", comparisons,
                "matches", matches
        );
        return new OperatorResult(
                joinedSchema,
                joinedRows,
                new OperatorMetrics("NestedLoopJoin", leftResult.rows().size() + rightResult.rows().size(),
                        joinedRows.size(), List.of(leftResult.metrics(), rightResult.metrics()), details)
        );
    }

    private static TableSchema joinedSchema(TableSchema left, TableSchema right, String leftTable, String rightTable) {
        List<ColumnDefinition> columns = new ArrayList<>();
        left.columns().forEach(column -> columns.add(new ColumnDefinition(leftTable + "." + column.name(), column.type())));
        right.columns().forEach(column -> columns.add(new ColumnDefinition(rightTable + "." + column.name(), column.type())));
        return new TableSchema(columns);
    }

    private static Row combine(
            TableSchema schema,
            Row leftRow,
            Row rightRow,
            TableSchema leftSchema,
            TableSchema rightSchema,
            String leftTable,
            String rightTable
    ) {
        Map<String, Object> values = new LinkedHashMap<>();
        leftSchema.columns().forEach(column -> values.put(leftTable + "." + column.name(), leftRow.get(column.name())));
        rightSchema.columns().forEach(column -> values.put(rightTable + "." + column.name(), rightRow.get(column.name())));
        return new Row(schema, values);
    }

    private static void ensureCompatible(DataType left, DataType right) {
        if (left != right) {
            throw new QueryExecutionException("Type mismatch in JOIN condition: cannot compare " + left + " with " + right + ".");
        }
    }

    private static boolean valuesEqual(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }
}
