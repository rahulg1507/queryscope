package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.ast.BooleanLiteral;
import com.queryscope.backend.engine.ast.ColumnExpression;
import com.queryscope.backend.engine.ast.ComparisonExpression;
import com.queryscope.backend.engine.ast.ComparisonOperator;
import com.queryscope.backend.engine.ast.Expression;
import com.queryscope.backend.engine.ast.NumberLiteral;
import com.queryscope.backend.engine.ast.StringLiteral;
import com.queryscope.backend.engine.storage.ColumnDefinition;
import com.queryscope.backend.engine.storage.DataType;
import com.queryscope.backend.engine.storage.Row;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FilterOperator implements QueryOperator {

    private final QueryOperator input;
    private final ComparisonExpression condition;
    private final String tableName;

    public FilterOperator(QueryOperator input, ComparisonExpression condition, String tableName) {
        this.input = input;
        this.condition = condition;
        this.tableName = tableName;
    }

    @Override
    public OperatorResult execute() {
        OperatorResult inputResult = input.execute();
        validateCondition(inputResult);
        List<Row> filtered = new ArrayList<>();
        for (Row row : inputResult.rows()) {
            if (matches(row)) {
                filtered.add(row);
            }
        }
        return new OperatorResult(
                inputResult.schema(),
                filtered,
                new OperatorMetrics("Filter", inputResult.rows().size(), filtered.size(), List.of(inputResult.metrics()))
        );
    }

    private void validateCondition(OperatorResult inputResult) {
        if (!(condition.left() instanceof ColumnExpression column)) {
            throw new QueryExecutionException("WHERE conditions must compare a table column to a literal.");
        }
        ColumnDefinition leftColumn = inputResult.schema().requireColumn(column.name(), tableName);
        DataType rightType = literalType(condition.right());
        if (leftColumn.type() != rightType) {
            throw new QueryExecutionException("Type mismatch: cannot compare " + leftColumn.type()
                    + " with " + rightType + ".");
        }
        if (leftColumn.type() != DataType.INTEGER && condition.operator() != ComparisonOperator.EQUAL
                && condition.operator() != ComparisonOperator.NOT_EQUAL) {
            throw new QueryExecutionException("Operator " + condition.operator()
                    + " is not supported for " + leftColumn.type() + " values.");
        }
    }

    private boolean matches(Row row) {
        Object left = row.get(((ColumnExpression) condition.left()).name());
        Object right = literalValue(condition.right());
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

    private static DataType literalType(Expression expression) {
        if (expression instanceof NumberLiteral) return DataType.INTEGER;
        if (expression instanceof StringLiteral) return DataType.STRING;
        if (expression instanceof BooleanLiteral) return DataType.BOOLEAN;
        throw new QueryExecutionException("WHERE conditions require a literal right-hand value.");
    }

    private static Object literalValue(Expression expression) {
        if (expression instanceof NumberLiteral number) return number.value();
        if (expression instanceof StringLiteral string) return string.value();
        if (expression instanceof BooleanLiteral bool) return bool.value();
        throw new QueryExecutionException("WHERE conditions require a literal right-hand value.");
    }
}
