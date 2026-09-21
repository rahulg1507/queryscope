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

final class JoinOperatorSupport {

    private JoinOperatorSupport() {
    }

    static ResolvedColumns resolve(
            OperatorResult left,
            OperatorResult right,
            JoinCondition condition,
            String leftTable,
            String rightTable
    ) {
        if (condition.operator() != com.queryscope.backend.engine.plan.PlanComparisonOperator.EQUAL) {
            throw new QueryExecutionException("Only equality JOIN conditions are supported.");
        }
        ColumnResolution leftColumn = left.schema().resolveColumn(condition.leftColumn(), leftTable);
        ColumnResolution rightColumn = right.schema().resolveColumn(condition.rightColumn(), rightTable);
        if (leftColumn.column().type() != rightColumn.column().type()) {
            throw new QueryExecutionException("Type mismatch in JOIN condition: cannot compare "
                    + leftColumn.column().type() + " with " + rightColumn.column().type() + ".");
        }
        return new ResolvedColumns(leftColumn, rightColumn);
    }

    static TableSchema joinedSchema(TableSchema left, TableSchema right, String leftTable, String rightTable) {
        List<ColumnDefinition> columns = new ArrayList<>();
        left.columns().forEach(column -> columns.add(new ColumnDefinition(leftTable + "." + column.name(), column.type())));
        right.columns().forEach(column -> columns.add(new ColumnDefinition(rightTable + "." + column.name(), column.type())));
        return new TableSchema(columns);
    }

    static Row combine(
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

    static boolean valuesEqual(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }

    record ResolvedColumns(ColumnResolution left, ColumnResolution right) {
    }
}
