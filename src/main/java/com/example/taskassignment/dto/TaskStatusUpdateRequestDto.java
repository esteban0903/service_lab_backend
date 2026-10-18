package com.example.taskassignment.dto;

import com.example.taskassignment.domain.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusUpdateRequestDto(
        @NotNull(message = "Status is required")
        TaskStatus status
) {
}
