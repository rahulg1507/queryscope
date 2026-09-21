package com.queryscope.backend.engine.storage;

public record ColumnResolution(
        String reference,
        String actualName,
        String outputName,
        ColumnDefinition column
) {
}
