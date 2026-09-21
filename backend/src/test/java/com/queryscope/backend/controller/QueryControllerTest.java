package com.queryscope.backend.controller;

import com.queryscope.backend.engine.ast.ColumnSelectItem;
import com.queryscope.backend.engine.ast.SelectStatement;
import com.queryscope.backend.engine.ast.TableReference;
import com.queryscope.backend.service.QueryParserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QueryController.class)
class QueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QueryParserService queryParserService;

    @Test
    void returnsParsedAst() throws Exception {
        given(queryParserService.parse("SELECT name FROM users"))
                .willReturn(new SelectStatement(
                        java.util.List.of(new ColumnSelectItem("name")),
                        new TableReference("users"), null));

        mockMvc.perform(post("/api/query/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"SELECT name FROM users\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type").value("SELECT"))
                .andExpect(jsonPath("$.columns[0].type").value("COLUMN"))
                .andExpect(jsonPath("$.columns[0].name").value("name"))
                .andExpect(jsonPath("$.from.type").value("TABLE"))
                .andExpect(jsonPath("$.from.name").value("users"));
    }

    @Test
    void returnsBadRequestForParserErrors() throws Exception {
        given(queryParserService.parse("SELECT FROM users"))
                .willThrow(new com.queryscope.backend.engine.parser.ParserException("Expected SELECT column at position 7"));

        mockMvc.perform(post("/api/query/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"SELECT FROM users\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Expected SELECT column at position 7"));
    }

    @Test
    void returnsBadRequestForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/query/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed request body"));
    }
}
