package com.example.taskassignment.controller;

import com.example.taskassignment.domain.Task;
import com.example.taskassignment.domain.TaskStatus;
import com.example.taskassignment.domain.Worker;
import com.example.taskassignment.repository.AssignmentDayRepository;
import com.example.taskassignment.repository.AssignmentRepository;
import com.example.taskassignment.repository.TaskRepository;
import com.example.taskassignment.repository.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@org.springframework.transaction.annotation.Transactional
@DisplayName("SkillController Integration Tests")
class SkillControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AssignmentDayRepository assignmentDayRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @BeforeEach
    void setUp() {
        assignmentDayRepository.deleteAll();
        assignmentRepository.deleteAll();
        taskRepository.deleteAll();
        workerRepository.deleteAll();

        // Worker 1: java, spring boot
        Worker w1 = new Worker("Alice", 8, Set.of("java", "spring boot"));
        // Worker 2: java, docker
        Worker w2 = new Worker("Bob", 8, Set.of("java", "docker"));
        workerRepository.saveAll(java.util.List.of(w1, w2));

        // Task 1: java
        Task t1 = new Task("Task 1", 4, TaskStatus.NOT_DONE, Set.of("java"));
        // Task 2: kubernetes
        Task t2 = new Task("Task 2", 8, TaskStatus.NOT_DONE, Set.of("kubernetes"));
        taskRepository.saveAll(java.util.List.of(t1, t2));
    }

    @Test
    @DisplayName("GET /api/skills without authentication returns 401")
    void getSkillCatalog_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/skills"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/skills with WORKER role returns 403")
    @WithMockUser(roles = "WORKER")
    void getSkillCatalog_workerRole_returns403() throws Exception {
        mockMvc.perform(get("/api/skills"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/skills with ADMIN role returns sorted catalog with counts")
    @WithMockUser(roles = "ADMIN")
    void getSkillCatalog_adminRole_returnsCatalog() throws Exception {
        mockMvc.perform(get("/api/skills"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].name").value("docker"))
                .andExpect(jsonPath("$[0].workerCount").value(1))
                .andExpect(jsonPath("$[0].taskCount").value(0))
                .andExpect(jsonPath("$[1].name").value("java"))
                .andExpect(jsonPath("$[1].workerCount").value(2))
                .andExpect(jsonPath("$[1].taskCount").value(1))
                .andExpect(jsonPath("$[2].name").value("kubernetes"))
                .andExpect(jsonPath("$[2].workerCount").value(0))
                .andExpect(jsonPath("$[2].taskCount").value(1))
                .andExpect(jsonPath("$[3].name").value("spring boot"))
                .andExpect(jsonPath("$[3].workerCount").value(1))
                .andExpect(jsonPath("$[3].taskCount").value(0));
    }

    @Test
    @DisplayName("POST /api/tasks/coverage without authentication returns 401")
    void getTaskCoverage_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/tasks/coverage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skills\":[\"java\"]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/tasks/coverage with WORKER role returns 403")
    @WithMockUser(roles = "WORKER")
    void getTaskCoverage_workerRole_returns403() throws Exception {
        mockMvc.perform(post("/api/tasks/coverage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skills\":[\"java\"]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/tasks/coverage with ADMIN role evaluates coverage properly")
    @WithMockUser(roles = "ADMIN")
    void getTaskCoverage_adminRole_covered() throws Exception {
        mockMvc.perform(post("/api/tasks/coverage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skills\":[\"java\",\"docker\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coveredBy").value(1))
                .andExpect(jsonPath("$.warning").doesNotExist())
                .andExpect(jsonPath("$.missingSkills", hasSize(0)))
                .andExpect(jsonPath("$.perSkill", hasSize(2)));
    }

    @Test
    @DisplayName("POST /api/tasks/coverage returns warning when coveredBy is 0")
    @WithMockUser(roles = "ADMIN")
    void getTaskCoverage_adminRole_uncovered() throws Exception {
        mockMvc.perform(post("/api/tasks/coverage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skills\":[\"docker\",\"spring boot\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coveredBy").value(0))
                .andExpect(jsonPath("$.warning").value("Each skill exists, but no single worker has all of them"));
    }
}
