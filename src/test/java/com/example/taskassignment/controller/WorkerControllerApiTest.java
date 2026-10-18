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

    @Autowired
    private com.example.taskassignment.repository.UserRepository userRepository;

    @Test
    @DisplayName("Worker created with credentials can log in and receives token with WORKER role and workerId")
    void createWorker_withCredentials_canLogin() throws Exception {
        WorkerRequestDto request = new WorkerRequestDto("Elena Demo", 8, Set.of("java"), "elena.new@demo.com", "Secret123!");

        String responseContent = mockMvc.perform(post("/api/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number workerId = objectMapper.readTree(responseContent).get("id").numberValue();

        // Worker can log in
        com.example.taskassignment.dto.LoginRequestDto loginRequest =
                new com.example.taskassignment.dto.LoginRequestDto("elena.new@demo.com", "Secret123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("WORKER"))
                .andExpect(jsonPath("$.workerId").value(workerId.intValue()));
    }

    @Test
    @DisplayName("Creating worker with duplicate email returns 409 Conflict")
    void createWorker_duplicateEmail_returns409() throws Exception {
        WorkerRequestDto request1 = new WorkerRequestDto("Worker One", 8, Set.of("java"), "dup@demo.com", "Secret123!");
        mockMvc.perform(post("/api/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        WorkerRequestDto request2 = new WorkerRequestDto("Worker Two", 6, Set.of("python"), "dup@demo.com", "Secret123!");
        mockMvc.perform(post("/api/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Providing email without password (or password without email) returns 400 Bad Request")
    void createWorker_partialCredentials_returns400() throws Exception {
        WorkerRequestDto emailOnly = new WorkerRequestDto("Partial Worker", 8, Set.of("java"), "partial@demo.com", null);
        mockMvc.perform(post("/api/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emailOnly)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        WorkerRequestDto passwordOnly = new WorkerRequestDto("Partial Worker 2", 8, Set.of("java"), null, "Secret123!");
        mockMvc.perform(post("/api/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passwordOnly)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Deleting a worker removes its associated user")
    void deleteWorker_removesAssociatedUser() throws Exception {
        WorkerRequestDto request = new WorkerRequestDto("User Bound Worker", 8, Set.of("java"), "bound@demo.com", "Secret123!");
        String responseContent = mockMvc.perform(post("/api/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number workerId = objectMapper.readTree(responseContent).get("id").numberValue();
        org.assertj.core.api.Assertions.assertThat(userRepository.findByEmail("bound@demo.com")).isPresent();

        mockMvc.perform(delete("/api/workers/" + workerId))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(userRepository.findByEmail("bound@demo.com")).isEmpty();
    }

    @Test
    @DisplayName("Worker without credentials still works without user account")
    void createWorker_withoutCredentials_worksWithoutUser() throws Exception {
        WorkerRequestDto request = new WorkerRequestDto("No Creds Worker", 8, Set.of("java"));
        String responseContent = mockMvc.perform(post("/api/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number workerId = objectMapper.readTree(responseContent).get("id").numberValue();
        org.assertj.core.api.Assertions.assertThat(userRepository.findByWorkerId(workerId.longValue())).isEmpty();
    }

    @Test
    @DisplayName("PUT /api/workers/{id}/credentials resets password and the old password stops working")
    void updateCredentials_resetsPasswordAndInvalidatesOldPassword() throws Exception {
        WorkerRequestDto request = new WorkerRequestDto("Reset Target Worker", 8, Set.of("java"), "reset@demo.com", "OldPassword123!");
        String responseContent = mockMvc.perform(post("/api/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number workerId = objectMapper.readTree(responseContent).get("id").numberValue();

        // Old login works
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.example.taskassignment.dto.LoginRequestDto("reset@demo.com", "OldPassword123!"))))
                .andExpect(status().isOk());

        // Reset credentials
        com.example.taskassignment.dto.WorkerCredentialsRequestDto creds =
                new com.example.taskassignment.dto.WorkerCredentialsRequestDto("reset@demo.com", "NewPassword123!");
        mockMvc.perform(put("/api/workers/" + workerId + "/credentials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creds)))
                .andExpect(status().isNoContent());

        // Old password now fails with 401
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.example.taskassignment.dto.LoginRequestDto("reset@demo.com", "OldPassword123!"))))
                .andExpect(status().isUnauthorized());

        // New password succeeds with 200
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.example.taskassignment.dto.LoginRequestDto("reset@demo.com", "NewPassword123!"))))
                .andExpect(status().isOk());
    }
}

