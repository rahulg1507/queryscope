package com.queryscope.backend.engine.ast;

public sealed interface SelectItem permits AggregateSelectItem, ColumnSelectItem, WildcardSelectItem {

    String getType();
}
