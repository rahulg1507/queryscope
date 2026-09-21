package com.queryscope.backend.engine.plan;

import com.queryscope.backend.engine.ast.BooleanLiteral;
import com.queryscope.backend.engine.ast.ColumnExpression;
import com.queryscope.backend.engine.ast.ColumnSelectItem;
import com.queryscope.backend.engine.ast.ComparisonExpression;
import com.queryscope.backend.engine.ast.ComparisonOperator;
import com.queryscope.backend.engine.ast.Expression;
import com.queryscope.backend.engine.ast.NumberLiteral;
import com.queryscope.backend.engine.ast.SelectItem;
import com.queryscope.backend.engine.ast.SelectStatement;
import com.queryscope.backend.engine.ast.StringLiteral;
import com.queryscope.backend.engine.ast.WildcardSelectItem;
import com.queryscope.backend.engine.execution.QueryExecutionException;
import com.queryscope.backend.engine.storage.DataType;
import com.queryscope.backend.engine.storage.Database;

import java.util.ArrayList;
import java.util.List;

public final class ExecutionPlanBuilder {

    private final Database database;

    public ExecutionPlanBuilder(Database database) {
        this.database = database;
    }

    public ExecutionPlan build(SelectStatement statement) {
        if (statement == null || statement.from() == null) {
            throw new QueryExecutionException("A SELECT statement requires a table.");
        }
        String tableName = statement.from().name();
        ExecutionPlanNode root = new TableScanPlan(tableName);

        if (statement.where() != null) {
            if (!(statement.where() instanceof ComparisonExpression comparison)) {
                throw new QueryExecutionException("Unsupported WHERE expression.");
            }
            root = new FilterPlan(tableName, toCondition(comparison), root);
        }

        List<String> projection = projectionColumns(statement.columns(), tableName);
        if (projection != null) {
            root = new ProjectionPlan(tableName, projection, root);
        }
        return new ExecutionPlan(root);
    }

    private List<String> projectionColumns(List<SelectItem> items, String tableName) {
        if (items.size() == 1 && items.get(0) instanceof WildcardSelectItem) {
            return null;
        }
        List<String> columns = new ArrayList<>();
        for (SelectItem item : items) {
            if (item instanceof WildcardSelectItem) {
                columns.addAll(database.requireTable(tableName).schema().columns().stream()
                        .map(column -> column.name())
                        .toList());
            } else if (item instanceof ColumnSelectItem column) {
                columns.add(column.name());
            } else {
                throw new QueryExecutionException("Unsupported SELECT item.");
            }
        }
        return columns;
    }

    private static FilterCondition toCondition(ComparisonExpression comparison) {
        if (!(comparison.left() instanceof ColumnExpression column)) {
            throw new QueryExecutionException("WHERE conditions must compare a table column to a literal.");
        }
        Expression right = comparison.right();
        if (right instanceof NumberLiteral number) {
            return new FilterCondition(column.name(), toOperator(comparison.operator()), number.value(), DataType.INTEGER);
        }
        if (right instanceof StringLiteral string) {
            return new FilterCondition(column.name(), toOperator(comparison.operator()), string.value(), DataType.STRING);
        }
        if (right instanceof BooleanLiteral bool) {
            return new FilterCondition(column.name(), toOperator(comparison.operator()), bool.value(), DataType.BOOLEAN);
        }
        throw new QueryExecutionException("WHERE conditions require a literal right-hand value.");
    }

    private static PlanComparisonOperator toOperator(ComparisonOperator operator) {
        return switch (operator) {
            case EQUAL -> PlanComparisonOperator.EQUAL;
            case NOT_EQUAL -> PlanComparisonOperator.NOT_EQUAL;
            case LESS_THAN -> PlanComparisonOperator.LESS_THAN;
            case LESS_EQUAL -> PlanComparisonOperator.LESS_EQUAL;
            case GREATER_THAN -> PlanComparisonOperator.GREATER_THAN;
            case GREATER_EQUAL -> PlanComparisonOperator.GREATER_EQUAL;
        };
    }
}
