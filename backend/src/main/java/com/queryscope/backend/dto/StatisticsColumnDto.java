package com.queryscope.backend.dto;

import com.queryscope.backend.engine.storage.DataType;

public record StatisticsColumnDto(
        String name,
        DataType type,
        long distinctValues,
        Object min,
        Object max
) {
}
