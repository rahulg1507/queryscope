package com.queryscope.backend.engine.storage;

import com.queryscope.backend.engine.execution.QueryExecutionException;

import java.util.Locale;

public enum DataType {
    INTEGER,
    STRING,
    BOOLEAN;

    public Object coerce(Object value, String columnName) {
        if (value == null) {
            throw new QueryExecutionException("Column '" + columnName + "' does not accept null values.");
        }
        return switch (this) {
            case INTEGER -> coerceInteger(value, columnName);
            case STRING -> {
                if (!(value instanceof String)) {
                    throw invalidType(columnName, this, value);
                }
                yield value;
            }
            case BOOLEAN -> {
                if (!(value instanceof Boolean)) {
                    throw invalidType(columnName, this, value);
                }
                yield value;
            }
        };
    }

    private static Long coerceInteger(Object value, String columnName) {
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return ((Number) value).longValue();
        }
        throw invalidType(columnName, INTEGER, value);
    }

    private static QueryExecutionException invalidType(String columnName, DataType expected, Object value) {
        return new QueryExecutionException("Invalid value for column '" + columnName + "': expected "
                + expected + " but got " + value.getClass().getSimpleName() + ".");
    }

    public static DataType fromName(String value) {
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new QueryExecutionException("Unsupported data type '" + value + "'.");
        }
    }
}
