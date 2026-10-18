package com.example.taskassignment.controller;

import com.example.taskassignment.domain.TaskStatus;
import com.example.taskassignment.dto.assignment.AssignmentRequestDto;
import com.example.taskassignment.dto.task.TaskRequestDto;
import com.example.taskassignment.dto.worker.WorkerRequestDto;
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

import java.time.LocalDate;
import java.util.Set;

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
@DisplayName("Assignment Flow & Section 6 Rules Integration Tests")
class AssignmentFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Full assignment flow: assign, check allocations, assign second task using free capacity, delete and verify capacity freed")
    void fullAssignmentLifecycle() throws Exception {
        // 1. Create a worker (8h/day, skills: [java, spring])
        WorkerRequestDto workerReq = new WorkerRequestDto("Bob Engineer", 8, Set.of("java", "spring"));
        String workerRes = mockMvc.perform(post("/api/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workerReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number workerId = objectMapper.readTree(workerRes).get("id").numberValue();

        // 2. Create Task 1 (6h, skills: [java])
        TaskRequestDto task1Req = new TaskRequestDto("Task One", 6, null, Set.of("java"));
        String task1Res = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task1Req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number task1Id = objectMapper.readTree(task1Res).get("id").numberValue();

        // 3. Assign Task 1 starting Thursday 2026-10-15
        LocalDate startThursday = LocalDate.of(2026, 10, 15);
        AssignmentRequestDto assign1Req = new AssignmentRequestDto(task1Id.longValue(), workerId.longValue(), startThursday);
        String assign1Res = mockMvc.perform(post("/api/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assign1Req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startDate").value("2026-10-15"))
                .andExpect(jsonPath("$.endDate").value("2026-10-15"))
                .andExpect(jsonPath("$.allocations", hasSize(1)))
                .andExpect(jsonPath("$.allocations[0].date").value("2026-10-15"))
                .andExpect(jsonPath("$.allocations[0].hours").value(6))
                .andReturn().getResponse().getContentAsString();
        Number assign1Id = objectMapper.readTree(assign1Res).get("id").numberValue();

        // Check worker schedule: Thursday should have 6h booked
        mockMvc.perform(get("/api/workers/" + workerId + "/schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['2026-10-15']").value(6));

        // 4. Create Task 2 (5h, skills: [spring])
        TaskRequestDto task2Req = new TaskRequestDto("Task Two", 5, null, Set.of("spring"));
        String task2Res = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task2Req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number task2Id = objectMapper.readTree(task2Res).get("id").numberValue();

        // 5. Assign Task 2 starting also on Thursday 2026-10-15
        // Thursday has only 8 - 6 = 2h free capacity! Remaining 3h must spill to Friday 2026-10-16.
        AssignmentRequestDto assign2Req = new AssignmentRequestDto(task2Id.longValue(), workerId.longValue(), startThursday);
        mockMvc.perform(post("/api/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assign2Req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startDate").value("2026-10-15"))
                .andExpect(jsonPath("$.endDate").value("2026-10-16"))
                .andExpect(jsonPath("$.allocations", hasSize(2)))
                .andExpect(jsonPath("$.allocations[0].date").value("2026-10-15"))
                .andExpect(jsonPath("$.allocations[0].hours").value(2))
                .andExpect(jsonPath("$.allocations[1].date").value("2026-10-16"))
                .andExpect(jsonPath("$.allocations[1].hours").value(3));

        // Verify updated worker schedule: Thursday has 8h (full), Friday has 3h
        mockMvc.perform(get("/api/workers/" + workerId + "/schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['2026-10-15']").value(8))
                .andExpect(jsonPath("$.['2026-10-16']").value(3));

        // 6. Delete Assignment 1 -> frees capacity
        mockMvc.perform(delete("/api/assignments/" + assign1Id))
                .andExpect(status().isNoContent());

        // Verify Thursday now only has Task 2's 2h
        mockMvc.perform(get("/api/workers/" + workerId + "/schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['2026-10-15']").value(2))
                .andExpect(jsonPath("$.['2026-10-16']").value(3));
    }

    @Test
    @DisplayName("Weekend startDate moves to Monday as effective start date")
    void weekendStartDate_movesToMonday() throws Exception {
        WorkerRequestDto workerReq = new WorkerRequestDto("Weekend Worker", 8, Set.of("java"));
        String workerRes = mockMvc.perform(post("/api/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workerReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number workerId = objectMapper.readTree(workerRes).get("id").numberValue();

        TaskRequestDto taskReq = new TaskRequestDto("Weekend Task", 4, null, Set.of("java"));
        String taskRes = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number taskId = objectMapper.readTree(taskRes).get("id").numberValue();

        // 2026-10-17 is Saturday -> effective start must be Monday 2026-10-19
        LocalDate saturday = LocalDate.of(2026, 10, 17);
        AssignmentRequestDto assignReq = new AssignmentRequestDto(taskId.longValue(), workerId.longValue(), saturday);

        mockMvc.perform(post("/api/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startDate").value("2026-10-19"))
                .andExpect(jsonPath("$.endDate").value("2026-10-19"))
                .andExpect(jsonPath("$.allocations[0].date").value("2026-10-19"))
                .andExpect(jsonPath("$.allocations[0].hours").value(4));
    }

    @Test
    @DisplayName("SPEC Section 6: Cannot modify effortHours or skills of an assigned task -> 409")
    void cannotModifyAssignedTaskEffortOrSkills() throws Exception {
        WorkerRequestDto workerReq = new WorkerRequestDto("Worker Alpha", 8, Set.of("java"));
        String workerRes = mockMvc.perform(post("/api/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workerReq)))
                .andReturn().getResponse().getContentAsString();
        Number workerId = objectMapper.readTree(workerRes).get("id").numberValue();

        TaskRequestDto taskReq = new TaskRequestDto("Task Alpha", 8, null, Set.of("java"));
        String taskRes = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskReq)))
                .andReturn().getResponse().getContentAsString();
        Number taskId = objectMapper.readTree(taskRes).get("id").numberValue();

        // Assign
        mockMvc.perform(post("/api/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignmentRequestDto(taskId.longValue(), workerId.longValue(), LocalDate.of(2026, 10, 15)))))
                .andExpect(status().isCreated());

        // Try to update effortHours -> 409
        TaskRequestDto changeEffort = new TaskRequestDto("Task Alpha", 10, null, Set.of("java"));
        mockMvc.perform(put("/api/tasks/" + taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeEffort)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message", containsString("Cannot modify effortHours or skills of an assigned task")));

        // Try to update skills -> 409
        TaskRequestDto changeSkills = new TaskRequestDto("Task Alpha", 8, null, Set.of("java", "sql"));
        mockMvc.perform(put("/api/tasks/" + taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeSkills)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // Update name and status only -> 200 OK allowed
        TaskRequestDto changeNameOnly = new TaskRequestDto("Task Alpha Renamed", 8, null, Set.of("java"));
        mockMvc.perform(put("/api/tasks/" + taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeNameOnly)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Task Alpha Renamed"));
    }

    @Test
    @DisplayName("SPEC Section 6: Deleting a worker with active assignments returns 409 Conflict")
    void cannotDeleteWorkerWithAssignments() throws Exception {
        WorkerRequestDto workerReq = new WorkerRequestDto("Worker Beta", 8, Set.of("java"));
        String workerRes = mockMvc.perform(post("/api/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workerReq)))
                .andReturn().getResponse().getContentAsString();
        Number workerId = objectMapper.readTree(workerRes).get("id").numberValue();

        TaskRequestDto taskReq = new TaskRequestDto("Task Beta", 4, null, Set.of("java"));
        String taskRes = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskReq)))
                .andReturn().getResponse().getContentAsString();
        Number taskId = objectMapper.readTree(taskRes).get("id").numberValue();

        // Assign
        mockMvc.perform(post("/api/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignmentRequestDto(taskId.longValue(), workerId.longValue(), LocalDate.of(2026, 10, 15)))))
                .andExpect(status().isCreated());

        // Try to delete worker -> 409
        mockMvc.perform(delete("/api/workers/" + workerId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message", containsString("Cannot delete worker with active assignments")));

        // Try to change worker hoursPerDay -> 409
        mockMvc.perform(put("/api/workers/" + workerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WorkerRequestDto("Worker Beta", 6, Set.of("java")))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("SPEC Section 6: Deleting an assigned task cascades and deletes assignment and days")
    void deleteTask_cascadesAssignment() throws Exception {
        WorkerRequestDto workerReq = new WorkerRequestDto("Worker Gamma", 8, Set.of("java"));
        String workerRes = mockMvc.perform(post("/api/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workerReq)))
                .andReturn().getResponse().getContentAsString();
        Number workerId = objectMapper.readTree(workerRes).get("id").numberValue();

        TaskRequestDto taskReq = new TaskRequestDto("Task Gamma", 4, null, Set.of("java"));
        String taskRes = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskReq)))
                .andReturn().getResponse().getContentAsString();
        Number taskId = objectMapper.readTree(taskRes).get("id").numberValue();

        mockMvc.perform(post("/api/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignmentRequestDto(taskId.longValue(), workerId.longValue(), LocalDate.of(2026, 10, 15)))))
                .andExpect(status().isCreated());

        // Delete task
        mockMvc.perform(delete("/api/tasks/" + taskId))
                .andExpect(status().isNoContent());

        // Worker should now have 0 assignments and can be safely deleted
        mockMvc.perform(delete("/api/workers/" + workerId))
                .andExpect(status().isNoContent());
    }
}

