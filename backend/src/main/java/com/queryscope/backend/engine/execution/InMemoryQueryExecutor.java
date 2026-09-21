package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.ast.ColumnSelectItem;
import com.queryscope.backend.engine.ast.SelectItem;
import com.queryscope.backend.engine.ast.SelectStatement;
import com.queryscope.backend.engine.ast.WildcardSelectItem;
import com.queryscope.backend.engine.storage.Database;

import java.util.List;
import java.util.ArrayList;

public final class InMemoryQueryExecutor implements QueryExecutor {

    private final Database database;

    public InMemoryQueryExecutor(Database database) {
        this.database = database;
    }

    @Override
    public QueryResult execute(SelectStatement statement) {
        String tableName = statement.from().name();
        QueryOperator operator = new TableScanOperator(database, tableName);
        if (statement.where() != null) {
            if (!(statement.where() instanceof com.queryscope.backend.engine.ast.ComparisonExpression comparison)) {
                throw new QueryExecutionException("Unsupported WHERE expression.");
            }
            operator = new FilterOperator(operator, comparison, tableName);
        }

        List<String> allColumns = database.requireTable(tableName).schema().columns().stream()
                .map(column -> column.name())
                .toList();
        List<String> projection = new ArrayList<>();
        for (SelectItem item : statement.columns()) {
            if (item instanceof WildcardSelectItem) {
                projection.addAll(allColumns);
            } else {
                projection.add(((ColumnSelectItem) item).name());
            }
        }
        operator = new ProjectionOperator(operator, projection, tableName);
        return QueryResult.from(operator.execute());
    }
}
