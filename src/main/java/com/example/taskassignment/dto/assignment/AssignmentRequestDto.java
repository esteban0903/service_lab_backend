package com.example.taskassignment.dto.assignment;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AssignmentRequestDto(
        @NotNull(message = "Task ID is required")
        Long taskId,

        @NotNull(message = "Worker ID is required")
        Long workerId,

        @NotNull(message = "Start date is required")
        LocalDate startDate
) {
}

