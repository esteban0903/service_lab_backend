package com.example.taskassignment.controller;

import com.example.taskassignment.domain.TaskStatus;
import com.example.taskassignment.dto.TaskRequestDto;
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
class TaskControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/tasks with valid body should return 201 and normalized task")
    void createTask_valid_returns201() throws Exception {
        TaskRequestDto request = new TaskRequestDto("  Setup Auth  ", 6, null, Set.of(" Java ", "SECURITY "));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Setup Auth"))
                .andExpect(jsonPath("$.effortHours").value(6))
                .andExpect(jsonPath("$.status").value("NOT_DONE"))
                .andExpect(jsonPath("$.skills", containsInAnyOrder("java", "security")));
    }

    @Test
    @DisplayName("POST /api/tasks with blank name should return 400 Bad Request with field error in message")
    void createTask_blankName_returns400() throws Exception {
        TaskRequestDto request = new TaskRequestDto("", 5, TaskStatus.NOT_DONE, Set.of("java"));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("name")));
    }

    @Test
    @DisplayName("POST /api/tasks with non-positive effortHours should return 400 Bad Request")
    void createTask_invalidEffort_returns400() throws Exception {
        TaskRequestDto request = new TaskRequestDto("Valid Name", 0, TaskStatus.NOT_DONE, Set.of("java"));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("effortHours")));
    }

    @Test
    @DisplayName("GET /api/tasks should return list of tasks")
    void getAllTasks_returns200() throws Exception {
        TaskRequestDto request = new TaskRequestDto("Task 1", 4, null, Set.of("java"));
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Task 1"));
    }

    @Test
    @DisplayName("GET /api/tasks/{id} should return 200 when found, 404 when not found")
    void getTaskById_checks() throws Exception {
        TaskRequestDto request = new TaskRequestDto("Task To Find", 8, null, Set.of("sql"));
        String responseContent = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number id = objectMapper.readTree(responseContent).get("id").numberValue();

        mockMvc.perform(get("/api/tasks/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Task To Find"));

        mockMvc.perform(get("/api/tasks/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message", containsString("Task not found with id: 99999")));
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} should update task when valid, return 404 when not found")
    void updateTask_checks() throws Exception {
        TaskRequestDto initial = new TaskRequestDto("Initial Name", 4, null, Set.of("java"));
        String responseContent = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initial)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number id = objectMapper.readTree(responseContent).get("id").numberValue();

        TaskRequestDto update = new TaskRequestDto("Updated Name", 10, TaskStatus.DONE, Set.of(" Kotlin "));
        mockMvc.perform(put("/api/tasks/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.effortHours").value(10))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.skills", containsInAnyOrder("kotlin")));

        mockMvc.perform(put("/api/tasks/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE /api/tasks/{id} should return 204 when deleted, 404 when not found")
    void deleteTask_checks() throws Exception {
        TaskRequestDto request = new TaskRequestDto("To Delete", 2, null, Set.of());
        String responseContent = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number id = objectMapper.readTree(responseContent).get("id").numberValue();

        mockMvc.perform(delete("/api/tasks/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/" + id))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/tasks/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}

