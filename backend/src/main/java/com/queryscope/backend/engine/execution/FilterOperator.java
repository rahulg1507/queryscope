package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.plan.FilterCondition;
import com.queryscope.backend.engine.plan.PlanComparisonOperator;
import com.queryscope.backend.engine.storage.ColumnDefinition;
import com.queryscope.backend.engine.storage.ColumnResolution;
import com.queryscope.backend.engine.storage.DataType;
import com.queryscope.backend.engine.storage.Row;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FilterOperator implements QueryOperator {

    private final QueryOperator input;
    private final FilterCondition condition;
    private final String tableName;

    public FilterOperator(QueryOperator input, FilterCondition condition, String tableName) {
        this.input = input;
        this.condition = condition;
        this.tableName = tableName;
    }

    @Override
    public OperatorResult execute() {
        OperatorResult inputResult = input.execute();
        ColumnResolution resolvedColumn = validateCondition(inputResult);
        List<Row> filtered = new ArrayList<>();
        for (Row row : inputResult.rows()) {
            if (matches(row, resolvedColumn.actualName())) {
                filtered.add(row);
            }
        }
        return new OperatorResult(
                inputResult.schema(),
                filtered,
                new OperatorMetrics("Filter", inputResult.rows().size(), filtered.size(), List.of(inputResult.metrics()))
        );
    }

    private ColumnResolution validateCondition(OperatorResult inputResult) {
        ColumnResolution resolvedColumn = inputResult.schema().resolveColumn(condition.column(), tableName);
        ColumnDefinition leftColumn = resolvedColumn.column();
        DataType rightType = condition.valueType();
        if (leftColumn.type() != rightType) {
            throw new QueryExecutionException("Type mismatch: cannot compare " + leftColumn.type()
                    + " with " + rightType + ".");
        }
        if (leftColumn.type() != DataType.INTEGER && condition.operator() != PlanComparisonOperator.EQUAL
                && condition.operator() != PlanComparisonOperator.NOT_EQUAL) {
                throw new QueryExecutionException("Operator " + condition.operator()
                        + " is not supported for " + leftColumn.type() + " values.");
        }
        return resolvedColumn;
    }

    private boolean matches(Row row, String actualColumnName) {
        Object left = row.get(actualColumnName);
        Object right = condition.value();
        int comparison = compare(left, right);
        return switch (condition.operator()) {
            case EQUAL -> comparison == 0;
            case NOT_EQUAL -> comparison != 0;
            case LESS_THAN -> comparison < 0;
            case LESS_EQUAL -> comparison <= 0;
            case GREATER_THAN -> comparison > 0;
            case GREATER_EQUAL -> comparison >= 0;
        };
    }

    private static int compare(Object left, Object right) {
        if (left instanceof Long leftNumber && right instanceof Long rightNumber) {
            return Long.compare(leftNumber, rightNumber);
        }
        if (left instanceof String leftText && right instanceof String rightText) {
            return leftText.compareTo(rightText);
        }
        if (left instanceof Boolean leftBoolean && right instanceof Boolean rightBoolean) {
            return Boolean.compare(leftBoolean, rightBoolean);
        }
        return Objects.equals(left, right) ? 0 : -1;
    }

}
