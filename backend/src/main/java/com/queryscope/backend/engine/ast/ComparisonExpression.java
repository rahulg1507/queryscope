package com.queryscope.backend.engine.ast;

public record ComparisonExpression(
        Expression left,
        ComparisonOperator operator,
        Expression right
) implements Expression {

    public ComparisonExpression {
        if (left == null || operator == null || right == null) {
            throw new IllegalArgumentException("Comparison expressions require both operands and an operator");
        }
    }

    @Override
    public String getType() {
        return "COMPARISON";
    }
}
