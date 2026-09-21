package com.queryscope.backend.dto;

import com.queryscope.backend.engine.storage.DataType;

public record SchemaColumnDto(String name, DataType type) {
}
