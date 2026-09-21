package com.queryscope.backend.engine.ast;

public record JoinCondition(ColumnExpression left, ColumnExpression right) {

    public JoinCondition {
        if (left == null || right == null) {
            throw new IllegalArgumentException("A join condition requires two column references");
        }
    }

    public String getType() {
        return "JOIN_CONDITION";
    }
}
