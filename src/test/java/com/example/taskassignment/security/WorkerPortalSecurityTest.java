package com.example.taskassignment.security;

import com.example.taskassignment.domain.Assignment;
import com.example.taskassignment.domain.Task;
import com.example.taskassignment.domain.TaskStatus;
import com.example.taskassignment.domain.Worker;
import com.example.taskassignment.dto.TaskStatusUpdateRequestDto;
import com.example.taskassignment.repository.AssignmentRepository;
import com.example.taskassignment.repository.TaskRepository;
import com.example.taskassignment.repository.WorkerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@DisplayName("Worker Portal & Access Control Security Tests")
class WorkerPortalSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    private Worker worker1;
    private Worker worker2;
    private Task task1;
    private Task task2;

    @BeforeEach
    void setUp() {
        worker1 = workerRepository.save(new Worker("Worker One", 8, Set.of("java")));
        worker2 = workerRepository.save(new Worker("Worker Two", 8, Set.of("java")));

        task1 = taskRepository.save(new Task("Task One", 4, TaskStatus.NOT_DONE, Set.of("java")));
        task2 = taskRepository.save(new Task("Task Two", 4, TaskStatus.NOT_DONE, Set.of("java")));

        assignmentRepository.save(new Assignment(task1, worker1, LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 19)));
        assignmentRepository.save(new Assignment(task2, worker2, LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 19)));
    }

    @Test
    @DisplayName("GET /api/me/assignments returns only assignments belonging to the logged-in worker")
    void getMyAssignments_returnsOnlyOwnAssignments() throws Exception {
        mockMvc.perform(get("/api/me/assignments")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WORKER"))
                                .jwt(token -> token.claim("workerId", worker1.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].worker.id", is(worker1.getId().intValue())));
    }

    @Test
    @DisplayName("PATCH /api/me/tasks/{taskId}/status successfully updates own task status to DONE")
    void updateTaskStatus_ownTask_returns200() throws Exception {
        TaskStatusUpdateRequestDto request = new TaskStatusUpdateRequestDto(TaskStatus.DONE);

        mockMvc.perform(patch("/api/me/tasks/" + task1.getId() + "/status")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WORKER"))
                                .jwt(token -> token.claim("workerId", worker1.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DONE")));
    }

    @Test
    @DisplayName("PATCH /api/me/tasks/{taskId}/status for another worker's task returns 403 Forbidden")
    void updateTaskStatus_otherWorkerTask_returns403() throws Exception {
        TaskStatusUpdateRequestDto request = new TaskStatusUpdateRequestDto(TaskStatus.DONE);

        // worker1 attempting to update task2 assigned to worker2
        mockMvc.perform(patch("/api/me/tasks/" + task2.getId() + "/status")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WORKER"))
                                .jwt(token -> token.claim("workerId", worker1.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("WORKER role attempting to access ADMIN-only endpoint /api/tasks returns 403 Forbidden")
    void workerRole_accessingAdminEndpoint_returns403() throws Exception {
        mockMvc.perform(get("/api/tasks")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WORKER"))
                                .jwt(token -> token.claim("workerId", worker1.getId()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }
}
