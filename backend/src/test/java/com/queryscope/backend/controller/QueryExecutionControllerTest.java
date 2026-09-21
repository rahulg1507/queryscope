package com.queryscope.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                .andExpect(jsonPath("$.metrics.rowsReturned").value(3));
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
}
