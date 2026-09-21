package com.queryscope.backend.engine.ast;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnore;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ColumnExpression(String qualifier, String name) implements Expression {

    public ColumnExpression(String name) {
        this(null, name);
    }

    public ColumnExpression {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Column name must not be blank");
        }
    }

    @Override
    public String getType() {
        return "COLUMN";
    }

    @JsonIgnore
    public boolean isQualified() {
        return qualifier != null && !qualifier.isBlank();
    }

    @JsonIgnore
    public String qualifiedName() {
        return isQualified() ? qualifier + "." + name : name;
    }
}
