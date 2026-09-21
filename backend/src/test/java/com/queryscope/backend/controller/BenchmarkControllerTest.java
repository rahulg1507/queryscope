package com.queryscope.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BenchmarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsSafeBenchmarkScenariosAndDatasetSizes() throws Exception {
        mockMvc.perform(get("/api/benchmarks/scenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenarios").isArray())
                .andExpect(jsonPath("$.scenarios").value(org.hamcrest.Matchers.hasItem("INDEX_EQUALITY")))
                .andExpect(jsonPath("$.scenarios").value(org.hamcrest.Matchers.hasItem("OPTIMIZER_JOIN")))
                .andExpect(jsonPath("$.datasetSizes").value(org.hamcrest.Matchers.contains("SMALL", "MEDIUM", "LARGE")));
    }

    @Test
    void runsBenchmarkWithoutTouchingApplicationDatabase() throws Exception {
        mockMvc.perform(post("/api/benchmarks/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenario\":\"INDEX_EQUALITY\",\"datasetSize\":\"SMALL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultsEquivalent").value(true))
                .andExpect(jsonPath("$.comparisons[0].strategy").value("TABLE_SCAN"))
                .andExpect(jsonPath("$.comparisons[1].strategy").value("INDEX_SCAN"))
                .andExpect(jsonPath("$.comparisons[0].actualMetrics.rowsScanned").value(1000));

        mockMvc.perform(get("/api/schema"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tables[*].name")
                        .value(org.hamcrest.Matchers.containsInAnyOrder("users", "expenses")))
                .andExpect(jsonPath("$.tables[?(@.name == 'expenses')].indexes[*].name")
                        .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("idx_benchmark_amount"))));
    }

    @Test
    void rejectsUnknownBenchmarkScenarioAndDatasetSize() throws Exception {
        mockMvc.perform(post("/api/benchmarks/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenario\":\"LATENCY\",\"datasetSize\":\"SMALL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Invalid benchmark scenario")));

        mockMvc.perform(post("/api/benchmarks/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenario\":\"INDEX_EQUALITY\",\"datasetSize\":\"HUGE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Invalid benchmark dataset size")));
    }
}
