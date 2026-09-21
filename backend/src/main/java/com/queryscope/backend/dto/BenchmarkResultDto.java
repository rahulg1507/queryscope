package com.queryscope.backend.dto;

import java.util.List;

public record BenchmarkResultDto(
        String scenario,
        String datasetSize,
        int rowsInvolved,
        List<String> strategiesCompared,
        boolean resultsEquivalent,
        List<BenchmarkComparisonDto> comparisons,
        BenchmarkOptimizerDto optimizer,
        String explanation
) {
}
