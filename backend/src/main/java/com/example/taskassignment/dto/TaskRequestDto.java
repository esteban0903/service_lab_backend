package com.example.taskassignment.dto;

import com.example.taskassignment.domain.TaskStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record TaskRequestDto(
        @NotBlank(message = "Task name is required")
        String name,

        @NotNull(message = "Effort hours is required")
        @Min(value = 1, message = "Effort hours must be greater than 0")
        Integer effortHours,

        TaskStatus status,

        Set<String> skills
) {
}

