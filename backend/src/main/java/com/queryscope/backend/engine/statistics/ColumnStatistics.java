package com.queryscope.backend.engine.statistics;

import com.queryscope.backend.engine.storage.DataType;

public record ColumnStatistics(
        String name,
        DataType type,
        long distinctValues,
        Object min,
        Object max
) {
}
