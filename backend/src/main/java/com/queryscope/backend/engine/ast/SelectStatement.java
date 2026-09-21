package com.queryscope.backend.engine.ast;

import java.util.List;

public record SelectStatement(
        List<SelectItem> columns,
        TableReference from,
        Expression where
) implements SqlStatement {

    public SelectStatement {
        columns = List.copyOf(columns);
    }

    public String getType() {
        return "SELECT";
    }
}
