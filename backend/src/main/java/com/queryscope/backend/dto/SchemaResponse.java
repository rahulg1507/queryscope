package com.queryscope.backend.dto;

import java.util.List;

public record SchemaResponse(List<SchemaTableDto> tables) {
}
