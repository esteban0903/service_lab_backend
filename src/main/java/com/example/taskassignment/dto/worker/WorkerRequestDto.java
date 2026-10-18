package com.example.taskassignment.dto.worker;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record WorkerRequestDto(
        @NotBlank(message = "Worker name is required")
        String name,

        @NotNull(message = "Hours per day is required")
        @Min(value = 1, message = "Hours per day must be between 1 and 24")
        @Max(value = 24, message = "Hours per day must be between 1 and 24")
        Integer hoursPerDay,

        Set<String> skills,

        String email,

        String temporaryPassword
) {
    public WorkerRequestDto(String name, Integer hoursPerDay, Set<String> skills) {
        this(name, hoursPerDay, skills, null, null);
    }
}

