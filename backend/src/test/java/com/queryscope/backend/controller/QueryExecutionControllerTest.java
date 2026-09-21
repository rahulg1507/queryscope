package com.queryscope.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class QueryExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void executesSelectAndReturnsRowsAndMetrics() throws Exception {
        mockMvc.perform(post("/api/query/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"SELECT name, age FROM users WHERE age > 18;\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns[0].name").value("name"))
                .andExpect(jsonPath("$.columns[1].type").value("INTEGER"))
                .andExpect(jsonPath("$.rows[0][0]").value("Rahul"))
                .andExpect(jsonPath("$.rows[2][0]").value("Maya"))
                .andExpect(jsonPath("$.rowCount").value(3))
                .andExpect(jsonPath("$.metrics.rowsScanned").value(4))
                .andExpect(jsonPath("$.metrics.rowsReturned").value(3))
                .andExpect(jsonPath("$.executionPlan.type").value("PROJECTION"))
                .andExpect(jsonPath("$.executionPlan.details.columns[0]").value("name"))
                .andExpect(jsonPath("$.executionPlan.inputRows").value(3))
                .andExpect(jsonPath("$.executionPlan.outputRows").value(3))
                .andExpect(jsonPath("$.executionPlan.children[0].type").value("FILTER"))
                .andExpect(jsonPath("$.executionPlan.children[0].details.condition").value("age > 18"))
                .andExpect(jsonPath("$.executionPlan.children[0].children[0].type").value("TABLE_SCAN"))
                .andExpect(jsonPath("$.executionPlan.children[0].children[0].details.table").value("users"));
    }

    @Test
    void returnsBadRequestForExecutionErrors() throws Exception {
        mockMvc.perform(post("/api/query/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"SELECT email FROM users\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown column 'email' in table 'users'."));
    }

    @Test
    void returnsBadRequestForMalformedRequest() throws Exception {
        mockMvc.perform(post("/api/query/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed request body"));
    }

    @Test
    void executesJoinAndReturnsBranchingPlanMetadata() throws Exception {
        mockMvc.perform(post("/api/query/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"SELECT users.name, expenses.amount FROM users JOIN expenses ON users.id = expenses.user_id;\",\"mode\":\"MANUAL\",\"joinStrategy\":\"NESTED_LOOP\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns[0].name").value("users.name"))
                .andExpect(jsonPath("$.columns[1].name").value("expenses.amount"))
                .andExpect(jsonPath("$.rows[0][0]").value("Rahul"))
                .andExpect(jsonPath("$.rows[1][1]").value(40))
                .andExpect(jsonPath("$.rowCount").value(4))
                .andExpect(jsonPath("$.executionPlan.children[0].type").value("NESTED_LOOP_JOIN"))
                .andExpect(jsonPath("$.executionPlan.children[0].details.condition").value("users.id = expenses.user_id"))
                .andExpect(jsonPath("$.executionPlan.children[0].details.comparisons").value(16))
                .andExpect(jsonPath("$.executionPlan.children[0].details.matches").value(4))
                .andExpect(jsonPath("$.executionPlan.children[0].children[0].type").value("TABLE_SCAN"))
                .andExpect(jsonPath("$.executionPlan.children[0].children[1].type").value("TABLE_SCAN"));
    }

    @Test
    void returnsBadRequestForAmbiguousJoinColumn() throws Exception {
        mockMvc.perform(post("/api/query/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"SELECT id FROM users JOIN expenses ON users.id = expenses.user_id;\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Ambiguous column 'id'."));
    }

    @Test
    void executesHashJoinWhenRequestedCaseInsensitively() throws Exception {
        mockMvc.perform(post("/api/query/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"SELECT users.name, expenses.amount FROM users JOIN expenses ON users.id = expenses.user_id;\",\"joinStrategy\":\"hash\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rowCount").value(4))
                .andExpect(jsonPath("$.executionPlan.children[0].type").value("HASH_JOIN"))
                .andExpect(jsonPath("$.executionPlan.children[0].details.strategy").value("HASH"))
                .andExpect(jsonPath("$.executionPlan.children[0].details.buildSide").value("LEFT"))
                .andExpect(jsonPath("$.executionPlan.children[0].details.hashLookups").value(4));
    }

    @Test
    void rejectsUnknownJoinStrategy() throws Exception {
        mockMvc.perform(post("/api/query/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"SELECT * FROM users JOIN expenses ON users.id = expenses.user_id\",\"joinStrategy\":\"sort_merge\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid join strategy 'sort_merge'. Supported strategies: NESTED_LOOP, HASH."));
    }

    @Test
    void defaultsToAutoModeAndExposesOptimizerTrace() throws Exception {
        mockMvc.perform(post("/api/query/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"SELECT users.name, expenses.amount FROM users JOIN expenses ON users.id = expenses.user_id\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.optimization.mode").value("AUTO"))
                .andExpect(jsonPath("$.optimization.rulesApplied[0].rule").value("EQUALITY_JOIN"))
                .andExpect(jsonPath("$.executionPlan.children[0].type").value("HASH_JOIN"));
    }

    @Test
    void acceptsNestedExecutionSettingsForManualMode() throws Exception {
        mockMvc.perform(post("/api/query/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"SELECT users.name, expenses.amount FROM users JOIN expenses ON users.id = expenses.user_id\",\"execution\":{\"mode\":\"MANUAL\",\"joinStrategy\":\"NESTED_LOOP\",\"scanStrategy\":\"TABLE\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.optimization.mode").value("MANUAL"))
                .andExpect(jsonPath("$.executionPlan.children[0].type").value("NESTED_LOOP_JOIN"));
    }

    @Test
    void rejectsUnknownExecutionMode() throws Exception {
        mockMvc.perform(post("/api/query/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"SELECT * FROM users\",\"mode\":\"COST_BASED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid execution mode 'COST_BASED'. Supported modes: AUTO, MANUAL."));
    }

    @Test
    void executesGroupedAggregateAndReturnsAggregatePlanMetadata() throws Exception {
        mockMvc.perform(post("/api/query/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"SELECT user_id, SUM(amount) FROM expenses GROUP BY user_id\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns[0].name").value("user_id"))
                .andExpect(jsonPath("$.columns[1].name").value("SUM(amount)"))
                .andExpect(jsonPath("$.rows[0][0]").value(1))
                .andExpect(jsonPath("$.rows[0][1]").value(130))
                .andExpect(jsonPath("$.rowCount").value(3))
                .andExpect(jsonPath("$.executionPlan.type").value("PROJECTION"))
                .andExpect(jsonPath("$.executionPlan.children[0].type").value("AGGREGATE"))
                .andExpect(jsonPath("$.executionPlan.children[0].details.groups").value(3))
                .andExpect(jsonPath("$.executionPlan.children[0].details.functions[0]").value("SUM(amount)"));
    }

    @Test
    void returnsBadRequestForInvalidGroupingSemantics() throws Exception {
        mockMvc.perform(post("/api/query/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"SELECT name, SUM(amount) FROM expenses GROUP BY user_id\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Column 'name' must appear in GROUP BY or be aggregated."));
    }

    @Test
    void exposesSchemaCreatesIndexAndExecutesIndexScan() throws Exception {
        mockMvc.perform(get("/api/schema"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tables[0].name").value("users"))
                .andExpect(jsonPath("$.tables[1].columns").isArray());

        mockMvc.perform(post("/api/schema/indexes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"idx_api_amount\",\"table\":\"expenses\",\"column\":\"amount\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tables[1].indexes[0].name").value("idx_api_amount"))
                .andExpect(jsonPath("$.tables[1].indexes[0].column").value("amount"));

        mockMvc.perform(post("/api/query/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"SELECT description FROM expenses WHERE amount = 300\",\"scanStrategy\":\"INDEX\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0][0]").value("Hotel"))
                .andExpect(jsonPath("$.executionPlan.children[0].type").value("INDEX_SCAN"))
                .andExpect(jsonPath("$.executionPlan.children[0].details.index").value("idx_api_amount"));
    }

    @Test
    void rejectsUnknownScanStrategyAndMissingUsableIndex() throws Exception {
        mockMvc.perform(post("/api/query/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"SELECT * FROM users WHERE age = 19\",\"mode\":\"MANUAL\",\"scanStrategy\":\"INDEX\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No usable index exists for predicate 'age = 19'."));

        mockMvc.perform(post("/api/query/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"SELECT * FROM users\",\"scanStrategy\":\"BITMAP\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid scan strategy 'BITMAP'. Supported strategies: TABLE, INDEX."));
    }
}
