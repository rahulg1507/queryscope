package com.queryscope.backend.engine.plan;

public record JoinCondition(
        String leftColumn,
        String rightColumn
) {
    public JoinCondition {
        if (leftColumn == null || leftColumn.isBlank() || rightColumn == null || rightColumn.isBlank()) {
            throw new IllegalArgumentException("A join condition requires two column references.");
        }
    }

    public String display() {
        return leftColumn + " = " + rightColumn;
    }
}
