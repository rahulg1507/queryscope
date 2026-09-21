package com.queryscope.backend.dto;

import java.util.List;

public record SchemaTableDto(
        String name,
        List<SchemaColumnDto> columns,
        List<SchemaIndexDto> indexes
) {
}
