package com.queryscope.backend.engine.ast;

public sealed interface SelectItem permits ColumnSelectItem, WildcardSelectItem {

    String getType();
}
