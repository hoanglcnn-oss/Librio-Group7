package com.librio.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /health returns status UP")
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("GET /resources returns seed items list")
    void testGetResourcesList() throws Exception {
        mockMvc.perform(get("/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(4));
    }

    @Test
    @DisplayName("GET /resources?q=refactoring returns matching item")
    void testSearchResource() throws Exception {
        mockMvc.perform(get("/resources").param("q", "Refactoring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(2))
                .andExpect(jsonPath("$.items[0].title").value("Refactoring"));
    }

    @Test
    @DisplayName("GET /resources/1 returns detail with physical and digital")
    void testGetResourceDetail_Found() throws Exception {
        mockMvc.perform(get("/resources/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.accessTypes[0]").value("PHYSICAL"))
                .andExpect(jsonPath("$.physical.totalCopies").value(5))
                .andExpect(jsonPath("$.physical.availableCopies").value(2))
                .andExpect(jsonPath("$.digital.available").value(true));
    }

    @Test
    @DisplayName("GET /resources/99999 returns 404 Resource not found")
    void testGetResourceDetail_NotFound() throws Exception {
        mockMvc.perform(get("/resources/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }
}
