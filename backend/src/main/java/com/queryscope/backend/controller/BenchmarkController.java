package com.queryscope.backend.controller;

import com.queryscope.backend.dto.BenchmarkCatalogResponse;
import com.queryscope.backend.dto.BenchmarkRequest;
import com.queryscope.backend.dto.BenchmarkResultDto;
import com.queryscope.backend.engine.benchmark.BenchmarkDatasetSize;
import com.queryscope.backend.engine.benchmark.BenchmarkScenario;
import com.queryscope.backend.engine.benchmark.BenchmarkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/benchmarks")
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    public BenchmarkController(BenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
    }

    @GetMapping("/scenarios")
    public BenchmarkCatalogResponse scenarios() {
        return new BenchmarkCatalogResponse(
                java.util.Arrays.stream(BenchmarkScenario.values()).map(Enum::name).toList(),
                java.util.Arrays.stream(BenchmarkDatasetSize.values()).map(Enum::name).toList()
        );
    }

    @PostMapping("/run")
    public BenchmarkResultDto run(@RequestBody BenchmarkRequest request) {
        if (request == null) {
            throw new com.queryscope.backend.engine.execution.QueryExecutionException(
                    "Benchmark request must include scenario and datasetSize."
            );
        }
        return benchmarkService.run(
                BenchmarkScenario.from(request.scenario()),
                BenchmarkDatasetSize.from(request.datasetSize())
        );
    }
}
