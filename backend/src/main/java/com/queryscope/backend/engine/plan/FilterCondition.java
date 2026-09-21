package com.queryscope.backend.engine.plan;

import com.queryscope.backend.engine.storage.DataType;

public record FilterCondition(
        String column,
        PlanComparisonOperator operator,
        Object value,
        DataType valueType
) {
    public FilterCondition {
        if (column == null || column.isBlank() || operator == null || value == null || valueType == null) {
            throw new IllegalArgumentException("Filter conditions require a column, operator, value, and value type.");
        }
    }

    public String display() {
        String renderedValue = valueType == DataType.STRING
                ? "'" + value.toString().replace("'", "''") + "'"
                : value.toString().toLowerCase();
        return column + " " + operator.symbol() + " " + renderedValue;
    }
}
