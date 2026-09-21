package com.queryscope.backend.engine.plan;

import com.queryscope.backend.engine.ast.BooleanLiteral;
import com.queryscope.backend.engine.ast.ColumnExpression;
import com.queryscope.backend.engine.ast.ColumnSelectItem;
import com.queryscope.backend.engine.ast.ComparisonExpression;
import com.queryscope.backend.engine.ast.ComparisonOperator;
import com.queryscope.backend.engine.ast.Expression;
import com.queryscope.backend.engine.ast.JoinSource;
import com.queryscope.backend.engine.ast.NumberLiteral;
import com.queryscope.backend.engine.ast.SelectItem;
import com.queryscope.backend.engine.ast.SelectStatement;
import com.queryscope.backend.engine.ast.StringLiteral;
import com.queryscope.backend.engine.ast.WildcardSelectItem;
import com.queryscope.backend.engine.execution.QueryExecutionException;
import com.queryscope.backend.engine.storage.DataType;

import java.util.ArrayList;
import java.util.List;

public final class ExecutionPlanBuilder {

    public ExecutionPlan build(SelectStatement statement) {
        return build(statement, JoinStrategy.NESTED_LOOP);
    }

    public ExecutionPlan build(SelectStatement statement, JoinStrategy joinStrategy) {
        if (statement == null || statement.from() == null) {
            throw new QueryExecutionException("A SELECT statement requires a table.");
        }
        if (joinStrategy == null) {
            joinStrategy = JoinStrategy.NESTED_LOOP;
        }
        ExecutionPlanNode root;
        String sourceContext;
        if (statement.from() instanceof com.queryscope.backend.engine.ast.TableReference table) {
            root = new TableScanPlan(table.name());
            sourceContext = table.name();
        } else if (statement.from() instanceof JoinSource join) {
            root = new JoinPlan(
                    join.left().name(),
                    join.right().name(),
                    new JoinCondition(join.condition().left().qualifiedName(), join.condition().right().qualifiedName()),
                    joinStrategy,
                    new TableScanPlan(join.left().name()),
                    new TableScanPlan(join.right().name())
            );
            sourceContext = join.left().name() + " JOIN " + join.right().name();
        } else {
            throw new QueryExecutionException("Unsupported FROM source.");
        }

        if (statement.where() != null) {
            if (!(statement.where() instanceof ComparisonExpression comparison)) {
                throw new QueryExecutionException("Unsupported WHERE expression.");
            }
            root = new FilterPlan(sourceContext, toCondition(comparison), root);
        }

        List<String> projection = projectionColumns(statement.columns());
        if (projection != null) {
            root = new ProjectionPlan(sourceContext, projection, root);
        }
        return new ExecutionPlan(root);
    }

    private List<String> projectionColumns(List<SelectItem> items) {
        if (items.size() == 1 && items.get(0) instanceof WildcardSelectItem) {
            return null;
        }
        List<String> columns = new ArrayList<>();
        for (SelectItem item : items) {
            if (item instanceof WildcardSelectItem) {
                throw new QueryExecutionException("Wildcard cannot be combined with other SELECT columns");
            } else if (item instanceof ColumnSelectItem column) {
                columns.add(column.qualifiedName());
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
