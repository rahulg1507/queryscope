package com.queryscope.backend.engine.ast;

public record TableReference(String name) implements FromSource {

    public TableReference {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Table name must not be blank");
        }
    }

    public String getType() {
        return "TABLE";
    }
}
