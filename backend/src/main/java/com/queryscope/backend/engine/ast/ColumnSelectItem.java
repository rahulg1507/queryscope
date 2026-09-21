package com.queryscope.backend.engine.ast;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnore;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ColumnSelectItem(String qualifier, String name) implements SelectItem {

    public ColumnSelectItem(String name) {
        this(null, name);
    }

    public ColumnSelectItem {
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
