package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.storage.DataType;

public record ResultColumn(String name, DataType type) {
}
