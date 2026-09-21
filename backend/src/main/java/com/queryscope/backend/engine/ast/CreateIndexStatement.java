package com.queryscope.backend.engine.ast;

public record CreateIndexStatement(
        String indexName,
        String tableName,
        String columnName
) implements SqlStatement {
    public CreateIndexStatement {
        if (indexName == null || indexName.isBlank() || tableName == null || tableName.isBlank()
                || columnName == null || columnName.isBlank()) {
            throw new IllegalArgumentException("CREATE INDEX requires an index, table, and column name.");
        }
    }

    public String getType() {
        return "CREATE_INDEX";
    }
}
