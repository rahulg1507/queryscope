package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.index.TableIndex;
import com.queryscope.backend.engine.index.TreeLookupResult;
import com.queryscope.backend.engine.plan.FilterCondition;
import com.queryscope.backend.engine.storage.ColumnResolution;
import com.queryscope.backend.engine.storage.Database;
import com.queryscope.backend.engine.storage.Row;
import com.queryscope.backend.engine.storage.Table;

import java.util.List;
import java.util.Map;

public final class IndexScanOperator implements QueryOperator {

    private final Database database;
    private final String tableName;
    private final FilterCondition condition;

    public IndexScanOperator(Database database, String tableName, FilterCondition condition) {
        this.database = database;
        this.tableName = tableName;
        this.condition = condition;
    }

    @Override
    public OperatorResult execute() {
        Table table = database.requireTable(tableName);
        ColumnResolution column = FilterPredicateSupport.validate(table.schema(), condition, tableName);
        TableIndex index = table.indexForColumn(column.actualName());
        if (index == null) {
            throw new QueryExecutionException("No usable index exists for predicate '" + condition.display() + "'.");
        }
        TreeLookupResult<Integer> lookup = index.lookup(condition);
        List<Row> tableRows = table.rows();
        List<Row> rows = lookup.values().stream().map(tableRows::get).toList();
        Map<String, Object> details = Map.of(
                "table", table.name(),
                "index", index.name(),
                "column", index.columnName(),
                "predicate", condition.display(),
                "indexLookups", 1,
                "leafEntriesVisited", lookup.leafEntriesVisited(),
                "rowsExamined", rows.size(),
                "rowsReturned", rows.size()
        );
        return new OperatorResult(
                table.schema(), rows,
                new OperatorMetrics("IndexScan", rows.size(), rows.size(), List.of(), details)
        );
    }
}
