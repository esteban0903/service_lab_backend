package com.example.taskassignment.controller;

import com.example.taskassignment.domain.TaskStatus;
import com.example.taskassignment.dto.AssignmentRequestDto;
import com.example.taskassignment.dto.AutoAssignmentRequestDto;
import com.example.taskassignment.dto.TaskRequestDto;
import com.example.taskassignment.dto.WorkerRequestDto;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@DisplayName("Auto-Assignment End-to-End Integration Tests")
class AutoAssignmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/assignments/auto end-to-end: verifies capacity limits, unassigned reporting, and existing assignments preserved")
    void autoAssign_endToEnd_verifiesCapacityLimitsAndState() throws Exception {
        LocalDate startDate = LocalDate.of(2026, 10, 15); // Thursday

        // 1. Create Worker 1 (capacity: 4h/day, skills: [java])
        String w1Res = mockMvc.perform(post("/api/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WorkerRequestDto("Worker One", 4, Set.of("java")))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long worker1Id = objectMapper.readTree(w1Res).get("id").asLong();

        // 2. Create Worker 2 (capacity: 8h/day, skills: [java, spring])
        String w2Res = mockMvc.perform(post("/api/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WorkerRequestDto("Worker Two", 8, Set.of("java", "spring")))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long worker2Id = objectMapper.readTree(w2Res).get("id").asLong();

        // 3. Create Task 1 (pre-assigned to Worker 1, 4h effort on Thursday)
        String tPreRes = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskRequestDto("Pre-assigned Task", 4, null, Set.of("java")))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long preAssignedTaskId = objectMapper.readTree(tPreRes).get("id").asLong();

        // Manually assign Task 1 to Worker 1
        mockMvc.perform(post("/api/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignmentRequestDto(preAssignedTaskId, worker1Id, startDate))))
                .andExpect(status().isCreated());

        // 4. Create Task 2 (DONE status -> must be ignored)
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskRequestDto("Done Task", 6, TaskStatus.DONE, Set.of("java")))))
                .andExpect(status().isCreated());

        // 5. Create Task 3 (unassigned, 8h, skills: [spring] -> only Worker 2 can do)
        String t3Res = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskRequestDto("Spring Task", 8, null, Set.of("spring")))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long springTaskId = objectMapper.readTree(t3Res).get("id").asLong();

        // 6. Create Task 4 (unassigned, 6h, skills: [java] -> flexible, can go to W1 or W2)
        String t4Res = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskRequestDto("Java Task", 6, null, Set.of("java")))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long javaTaskId = objectMapper.readTree(t4Res).get("id").asLong();

        // 7. Create Task 5 (unassigned, 4h, skills: [rust] -> impossible)
        String t5Res = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskRequestDto("Rust Task", 4, null, Set.of("rust")))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long rustTaskId = objectMapper.readTree(t5Res).get("id").asLong();

        // 8. Call POST /api/assignments/auto
        String autoRes = mockMvc.perform(post("/api/assignments/auto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AutoAssignmentRequestDto(startDate))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigned", hasSize(2)))
                .andExpect(jsonPath("$.unassigned", hasSize(1)))
                .andReturn().getResponse().getContentAsString();

        JsonNode autoResult = objectMapper.readTree(autoRes);

        // Verify unassigned reports Task 5 with reason
        assertThat(autoResult.get("unassigned").get(0).get("task").get("id").asLong()).isEqualTo(rustTaskId);
        assertThat(autoResult.get("unassigned").get(0).get("reason").asText()).isEqualTo("No worker has skills: [rust]");

        // 9. Call GET /api/assignments: should have 3 total assignments (1 pre-assigned + 2 auto-assigned)
        mockMvc.perform(get("/api/assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        // 10. Check Worker 1 schedule: no day must exceed 4h
        String w1ScheduleRes = mockMvc.perform(get("/api/workers/" + worker1Id + "/schedule"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode w1Schedule = objectMapper.readTree(w1ScheduleRes);
        Iterator<Map.Entry<String, JsonNode>> w1Fields = w1Schedule.fields();
        while (w1Fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = w1Fields.next();
            int hours = entry.getValue().asInt();
            assertThat(hours).as("Worker 1 daily hours on %s must not exceed 4", entry.getKey()).isLessThanOrEqualTo(4);
        }

        // 11. Check Worker 2 schedule: no day must exceed 8h
        String w2ScheduleRes = mockMvc.perform(get("/api/workers/" + worker2Id + "/schedule"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode w2Schedule = objectMapper.readTree(w2ScheduleRes);
        Iterator<Map.Entry<String, JsonNode>> w2Fields = w2Schedule.fields();
        while (w2Fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = w2Fields.next();
            int hours = entry.getValue().asInt();
            assertThat(hours).as("Worker 2 daily hours on %s must not exceed 8", entry.getKey()).isLessThanOrEqualTo(8);
        }
    }
}

