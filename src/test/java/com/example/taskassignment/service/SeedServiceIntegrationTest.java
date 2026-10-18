package com.example.taskassignment.service;

import com.example.taskassignment.dto.assignment.AutoAssignmentResultDto;
import com.example.taskassignment.dto.common.SeedSummaryDto;
import com.example.taskassignment.exception.ResourceNotFoundException;
import com.example.taskassignment.repository.AssignmentRepository;
import com.example.taskassignment.repository.TaskRepository;
import com.example.taskassignment.repository.WorkerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@com.example.taskassignment.security.WithMockAdmin
@DisplayName("SeedService and Demo Data Tests")
class SeedServiceIntegrationTest {

    @Autowired
    private SeedService seedService;

    @Autowired
    private AutoAssignmentService autoAssignmentService;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Seed creates 5 workers, 12 tasks, and 2 manual assignments; idempotent on repeated runs")
    void seed_createsExpectedCountsAndIsIdempotent() {
        SeedSummaryDto firstRun = seedService.seed();
        assertThat(firstRun.workersCreated()).isEqualTo(5);
        assertThat(firstRun.tasksCreated()).isEqualTo(12);
        assertThat(firstRun.assignmentsCreated()).isEqualTo(2);
        assertThat(firstRun.demoStartDate()).isNotNull();

        assertThat(workerRepository.count()).isEqualTo(5);
        assertThat(taskRepository.count()).isEqualTo(12);
        assertThat(assignmentRepository.count()).isEqualTo(2);

        // Run second time: must delete and repopulate cleanly without duplicates
        SeedSummaryDto secondRun = seedService.seed();
        assertThat(secondRun.workersCreated()).isEqualTo(5);
        assertThat(secondRun.tasksCreated()).isEqualTo(12);
        assertThat(secondRun.assignmentsCreated()).isEqualTo(2);
        assertThat(secondRun.demoStartDate()).isEqualTo(firstRun.demoStartDate());

        assertThat(workerRepository.count()).isEqualTo(5);
        assertThat(taskRepository.count()).isEqualTo(12);
        assertThat(assignmentRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Auto-assigning demo data produces at least one unassigned task with each of the two required reasons")
    void seedData_whenAutoAssigned_producesBothUnassignedReasons() {
        seedService.seed();

        AutoAssignmentResultDto result = autoAssignmentService.autoAssign(null);

        assertThat(result.assigned()).isNotEmpty();
        assertThat(result.unassigned()).hasSizeGreaterThanOrEqualTo(2);

        // 1. Skill nobody has (kotlin)
        assertThat(result.unassigned())
                .anySatisfy(unassigned -> {
                    assertThat(unassigned.task().name()).isEqualTo("Mobile Android Client");
                    assertThat(unassigned.reason()).isEqualTo("No worker has skills: [kotlin]");
                });

        // 2. Skills exist separately but no single worker has both (python, react)
        assertThat(result.unassigned())
                .anySatisfy(unassigned -> {
                    assertThat(unassigned.task().name()).isEqualTo("Fullstack Analytics Dashboard");
                    assertThat(unassigned.reason()).isEqualTo("No single worker has all required skills: [python, react]");
                });
    }

    @Test
    @DisplayName("POST /api/seed endpoint returns 200 with summary when enabled")
    void postSeedEndpoint_whenEnabled_returns200() throws Exception {
        mockMvc.perform(post("/api/seed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workersCreated").value(5))
                .andExpect(jsonPath("$.tasksCreated").value(12))
                .andExpect(jsonPath("$.assignmentsCreated").value(2))
                .andExpect(jsonPath("$.demoStartDate").isNotEmpty());
    }

    @Test
    @DisplayName("SeedService throws ResourceNotFoundException (404) when seedEnabled is false")
    void seed_whenDisabled_throwsResourceNotFound() {
        SeedService disabledSeedService = new SeedService(
                workerRepository,
                taskRepository,
                assignmentRepository,
                null,
                null,
                null,
                null,
                null,
                false
        );

        assertThatThrownBy(disabledSeedService::seed)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Seed endpoint is disabled");
    }

    @Autowired
    private com.example.taskassignment.repository.UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Seed never deletes or modifies ADMIN users")
    void seed_neverDeletesOrModifiesAdminUsers() {
        // Ensure admin user exists
        com.example.taskassignment.domain.User admin = userRepository.findByEmail("admin@demo.com")
                .orElseGet(() -> userRepository.save(new com.example.taskassignment.domain.User(
                        "admin@demo.com",
                        passwordEncoder.encode("Admin123!"),
                        com.example.taskassignment.domain.Role.ADMIN,
                        null
                )));
        String initialPasswordHash = admin.getPasswordHash();

        // Run seed multiple times
        seedService.seed();
        seedService.seed();

        // Admin must still exist, untouched
        com.example.taskassignment.domain.User preservedAdmin = userRepository.findByEmail("admin@demo.com")
                .orElseThrow(() -> new AssertionError("Admin user was deleted by seed!"));

        assertThat(preservedAdmin.getRole()).isEqualTo(com.example.taskassignment.domain.Role.ADMIN);
        assertThat(preservedAdmin.getPasswordHash()).isEqualTo(initialPasswordHash);
        assertThat(preservedAdmin.isEnabled()).isTrue();
        assertThat(preservedAdmin.getWorker()).isNull();
    }
}

