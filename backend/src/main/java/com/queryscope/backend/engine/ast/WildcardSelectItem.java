package com.queryscope.backend.engine.ast;

public record WildcardSelectItem() implements SelectItem {

    @Override
    public String getType() {
        return "WILDCARD";
    }
}
