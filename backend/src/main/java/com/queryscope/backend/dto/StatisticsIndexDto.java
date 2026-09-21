package com.queryscope.backend.dto;

public record StatisticsIndexDto(
        String name,
        String column,
        int distinctKeys,
        int indexedRows
) {
}
