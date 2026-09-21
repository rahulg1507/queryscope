package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.storage.Database;
import com.queryscope.backend.engine.storage.Table;

public final class TableScanOperator implements QueryOperator {

    private final Database database;
    private final String tableName;

    public TableScanOperator(Database database, String tableName) {
        this.database = database;
        this.tableName = tableName;
    }

    @Override
    public OperatorResult execute() {
        Table table = database.requireTable(tableName);
        int rowCount = table.rows().size();
        return new OperatorResult(
                table.schema(),
                table.rows(),
                new OperatorMetrics("TableScan", rowCount, rowCount, java.util.List.of())
        );
    }
}
