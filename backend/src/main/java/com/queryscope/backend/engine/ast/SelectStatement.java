package com.queryscope.backend.engine.ast;

import java.util.List;

public record SelectStatement(
        List<SelectItem> columns,
        FromSource from,
        Expression where,
        List<ColumnExpression> groupBy
) implements SqlStatement {

    public SelectStatement(List<SelectItem> columns, FromSource from, Expression where) {
        this(columns, from, where, List.of());
    }

    public SelectStatement {
        columns = List.copyOf(columns);
        groupBy = groupBy == null ? List.of() : List.copyOf(groupBy);
    }

    public String getType() {
        return "SELECT";
    }
}
