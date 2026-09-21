package com.queryscope.backend.engine.plan;

public record JoinCondition(
        String leftColumn,
        String rightColumn,
        PlanComparisonOperator operator
) {
    public JoinCondition(String leftColumn, String rightColumn) {
        this(leftColumn, rightColumn, PlanComparisonOperator.EQUAL);
    }

    public JoinCondition {
        if (leftColumn == null || leftColumn.isBlank() || rightColumn == null || rightColumn.isBlank()
                || operator == null) {
            throw new IllegalArgumentException("A join condition requires two column references.");
        }
    }

    public String display() {
        return leftColumn + " " + operator.symbol() + " " + rightColumn;
    }
}
