package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.storage.Row;
import com.queryscope.backend.engine.storage.TableSchema;

import java.util.List;

public record OperatorResult(TableSchema schema, List<Row> rows, OperatorMetrics metrics) {
    public OperatorResult {
        rows = List.copyOf(rows);
    }
}
