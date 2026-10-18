package com.example.taskassignment.controller;

import com.example.taskassignment.dto.WorkerRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@com.example.taskassignment.security.WithMockAdmin
class WorkerControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/workers with valid body should return 201 and normalized worker")
    void createWorker_valid_returns201() throws Exception {
        WorkerRequestDto request = new WorkerRequestDto("  John Doe  ", 8, Set.of(" Java ", "SPRING "));

        mockMvc.perform(post("/api/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.hoursPerDay").value(8))
                .andExpect(jsonPath("$.skills", containsInAnyOrder("java", "spring")));
    }

    @Test
    @DisplayName("POST /api/workers with blank name should return 400 Bad Request")
    void createWorker_blankName_returns400() throws Exception {
        WorkerRequestDto request = new WorkerRequestDto("  ", 8, Set.of("java"));

        mockMvc.perform(post("/api/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("name")));
    }

    @Test
    @DisplayName("POST /api/workers with hoursPerDay < 1 should return 400 Bad Request")
    void createWorker_hoursBelowMin_returns400() throws Exception {
        WorkerRequestDto request = new WorkerRequestDto("Jane Doe", 0, Set.of());

        mockMvc.perform(post("/api/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("hoursPerDay")));
    }

    @Test
    @DisplayName("POST /api/workers with hoursPerDay > 24 should return 400 Bad Request")
    void createWorker_hoursAboveMax_returns400() throws Exception {
        WorkerRequestDto request = new WorkerRequestDto("Jane Doe", 25, Set.of());

        mockMvc.perform(post("/api/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("hoursPerDay")));
    }

    @Test
    @DisplayName("GET /api/workers should return list of workers")
    void getAllWorkers_returns200() throws Exception {
        WorkerRequestDto request = new WorkerRequestDto("Worker 1", 6, Set.of("react"));
        mockMvc.perform(post("/api/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/workers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Worker 1"));
    }

    @Test
    @DisplayName("GET /api/workers/{id} should return 200 when found, 404 when not found")
    void getWorkerById_checks() throws Exception {
        WorkerRequestDto request = new WorkerRequestDto("Worker To Find", 7, Set.of("python"));
        String responseContent = mockMvc.perform(post("/api/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number id = objectMapper.readTree(responseContent).get("id").numberValue();

        mockMvc.perform(get("/api/workers/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Worker To Find"));

        mockMvc.perform(get("/api/workers/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString("Worker not found with id: 99999")));
    }

    @Test
    @DisplayName("PUT /api/workers/{id} should update worker when valid, return 404 when not found")
    void updateWorker_checks() throws Exception {
        WorkerRequestDto initial = new WorkerRequestDto("Initial Worker", 4, Set.of("java"));
        String responseContent = mockMvc.perform(post("/api/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initial)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number id = objectMapper.readTree(responseContent).get("id").numberValue();

        WorkerRequestDto update = new WorkerRequestDto("Updated Worker", 8, Set.of(" Docker "));
        mockMvc.perform(put("/api/workers/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Worker"))
                .andExpect(jsonPath("$.hoursPerDay").value(8))
                .andExpect(jsonPath("$.skills", containsInAnyOrder("docker")));

        mockMvc.perform(put("/api/workers/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE /api/workers/{id} should return 204 when deleted, 404 when not found")
    void deleteWorker_checks() throws Exception {
        WorkerRequestDto request = new WorkerRequestDto("Worker To Delete", 5, Set.of());
        String responseContent = mockMvc.perform(post("/api/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number id = objectMapper.readTree(responseContent).get("id").numberValue();

        mockMvc.perform(delete("/api/workers/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/workers/" + id))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/workers/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}

