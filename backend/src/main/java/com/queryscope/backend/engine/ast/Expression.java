package com.queryscope.backend.engine.ast;

public sealed interface Expression permits ColumnExpression, ComparisonExpression,
        NumberLiteral, StringLiteral, BooleanLiteral {

    String getType();
}
