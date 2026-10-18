package com.example.taskassignment.dto.common;

import java.time.LocalDate;

public record SeedSummaryDto(
        int workersCreated,
        int tasksCreated,
        int assignmentsCreated,
        LocalDate demoStartDate
) {
}
