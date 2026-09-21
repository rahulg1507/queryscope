package com.queryscope.backend.dto;

import java.util.List;

public record StatisticsTableDto(
        String name,
        long rowCount,
        List<StatisticsColumnDto> columns,
        List<StatisticsIndexDto> indexes
) {
}
