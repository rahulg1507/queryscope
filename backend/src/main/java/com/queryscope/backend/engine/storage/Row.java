package com.queryscope.backend.engine.storage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

public final class Row {

    private final TableSchema schema;
    private final Map<String, Object> values;

    public Row(TableSchema schema, Map<String, ?> values) {
        this.schema = schema;
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(schema.validateAndOrder(values)));
    }

    public Object get(String columnName) {
        ColumnDefinition column = schema.requireColumn(columnName, "row");
        return values.get(column.name());
    }

    public Map<String, Object> values() {
        return values;
    }

    public TableSchema schema() {
        return schema;
    }
}
