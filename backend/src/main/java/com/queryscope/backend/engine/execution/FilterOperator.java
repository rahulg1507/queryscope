package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.plan.FilterCondition;
import com.queryscope.backend.engine.plan.PlanComparisonOperator;
import com.queryscope.backend.engine.storage.ColumnResolution;
import com.queryscope.backend.engine.storage.Row;

import java.util.ArrayList;
import java.util.List;

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
        ColumnResolution resolvedColumn = FilterPredicateSupport.validate(inputResult.schema(), condition, tableName);
        List<Row> filtered = new ArrayList<>();
        for (Row row : inputResult.rows()) {
            if (FilterPredicateSupport.matches(row, resolvedColumn.actualName(), condition)) {
                filtered.add(row);
            }
        }
        return new OperatorResult(
                inputResult.schema(),
                filtered,
                new OperatorMetrics("Filter", inputResult.rows().size(), filtered.size(), List.of(inputResult.metrics()))
        );
    }

}
