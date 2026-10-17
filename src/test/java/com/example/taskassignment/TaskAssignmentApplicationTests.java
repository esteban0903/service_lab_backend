package com.example.taskassignment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class TaskAssignmentApplicationTests {

    @Test
    void contextLoads() {
        // Smoke test: verifies application context starts up and database migrations execute successfully.
    }
}

