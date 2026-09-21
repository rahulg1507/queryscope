package com.queryscope.backend.engine.ast;

public record JoinSource(
        TableReference left,
        TableReference right,
        JoinCondition condition
) implements FromSource {

    public JoinSource {
        if (left == null || right == null || condition == null) {
            throw new IllegalArgumentException("A join source requires two tables and a condition");
        }
    }

    public String getType() {
        return "JOIN";
    }
}
