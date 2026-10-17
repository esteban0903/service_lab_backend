package com.example.taskassignment.dto;

import com.example.taskassignment.domain.Worker;

import java.util.Set;

public record WorkerResponseDto(
        Long id,
        String name,
        int hoursPerDay,
        Set<String> skills
) {
    public static WorkerResponseDto fromEntity(Worker worker) {
        return new WorkerResponseDto(
                worker.getId(),
                worker.getName(),
                worker.getHoursPerDay(),
                worker.getSkills()
        );
    }
}

