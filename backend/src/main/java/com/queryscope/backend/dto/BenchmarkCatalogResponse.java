package com.queryscope.backend.dto;

import java.util.List;

public record BenchmarkCatalogResponse(List<String> scenarios, List<String> datasetSizes) {
}
