package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.plan.JoinCondition;
import com.queryscope.backend.engine.storage.ColumnResolution;
import com.queryscope.backend.engine.storage.Row;
import com.queryscope.backend.engine.storage.TableSchema;

import java.util.ArrayList;
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
        JoinOperatorSupport.ResolvedColumns columns = JoinOperatorSupport.resolve(
                leftResult, rightResult, condition, leftTable, rightTable
        );

        TableSchema joinedSchema = JoinOperatorSupport.joinedSchema(
                leftResult.schema(), rightResult.schema(), leftTable, rightTable
        );
        List<Row> joinedRows = new ArrayList<>();
        int comparisons = 0;
        int matches = 0;
        for (Row leftRow : leftResult.rows()) {
            for (Row rightRow : rightResult.rows()) {
                comparisons++;
                if (JoinOperatorSupport.valuesEqual(
                        leftRow.get(columns.left().actualName()), rightRow.get(columns.right().actualName()))) {
                    joinedRows.add(JoinOperatorSupport.combine(
                            joinedSchema, leftRow, rightRow, leftResult.schema(), rightResult.schema(), leftTable, rightTable
                    ));
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

}
