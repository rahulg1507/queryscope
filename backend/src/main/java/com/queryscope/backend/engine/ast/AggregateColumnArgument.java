package com.queryscope.backend.engine.ast;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnore;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AggregateColumnArgument(String qualifier, String name) implements AggregateArgument {

    public AggregateColumnArgument(String name) {
        this(null, name);
    }

    public AggregateColumnArgument {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Aggregate column name must not be blank.");
        }
    }

    @JsonIgnore
    public boolean isQualified() {
        return qualifier != null && !qualifier.isBlank();
    }

    @JsonIgnore
    public String qualifiedName() {
        return isQualified() ? qualifier + "." + name : name;
    }

    @Override
    public String display() {
        return qualifiedName();
    }
}
