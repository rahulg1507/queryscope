package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.plan.FilterCondition;
import com.queryscope.backend.engine.plan.PlanComparisonOperator;
import com.queryscope.backend.engine.storage.ColumnDefinition;
import com.queryscope.backend.engine.storage.ColumnResolution;
import com.queryscope.backend.engine.storage.DataType;
import com.queryscope.backend.engine.storage.Row;
import com.queryscope.backend.engine.storage.TableSchema;

import java.util.Objects;

final class FilterPredicateSupport {

    private FilterPredicateSupport() {
    }

    static ColumnResolution validate(TableSchema schema, FilterCondition condition, String tableName) {
        ColumnResolution resolvedColumn = schema.resolveColumn(condition.column(), tableName);
        ColumnDefinition leftColumn = resolvedColumn.column();
        if (leftColumn.type() != condition.valueType()) {
            throw new QueryExecutionException("Type mismatch: cannot compare " + leftColumn.type()
                    + " with " + condition.valueType() + ".");
        }
        if (leftColumn.type() != DataType.INTEGER && condition.operator() != PlanComparisonOperator.EQUAL
                && condition.operator() != PlanComparisonOperator.NOT_EQUAL) {
            throw new QueryExecutionException("Operator " + condition.operator()
                    + " is not supported for " + leftColumn.type() + " values.");
        }
        return resolvedColumn;
    }

    static boolean matches(Row row, String actualColumnName, FilterCondition condition) {
        int comparison = compare(row.get(actualColumnName), condition.value());
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
        if (left instanceof Number leftNumber && right instanceof Number rightNumber) {
            return Long.compare(leftNumber.longValue(), rightNumber.longValue());
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
