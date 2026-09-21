package com.queryscope.backend.engine.statistics;

public record IndexStatistics(
        String name,
        String column,
        int distinctKeys,
        int indexedRows
) {
}
