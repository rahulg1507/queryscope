package com.queryscope.backend.dto;

public record ExecutionSettings(
        String mode,
        String joinStrategy,
        String scanStrategy
) {
}
