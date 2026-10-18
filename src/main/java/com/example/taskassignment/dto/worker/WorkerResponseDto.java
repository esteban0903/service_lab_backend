package com.example.taskassignment.dto.worker;

import com.example.taskassignment.domain.Worker;

import java.util.Set;

public record WorkerResponseDto(
        Long id,
        String name,
        int hoursPerDay,
        Set<String> skills,
        String email,
        boolean hasLogin
) {
    public WorkerResponseDto(Long id, String name, int hoursPerDay, Set<String> skills) {
        this(id, name, hoursPerDay, skills, null, false);
    }

    public static WorkerResponseDto fromEntity(Worker worker) {
        return fromEntity(worker, null);
    }

    public static WorkerResponseDto fromEntity(Worker worker, String email) {
        return new WorkerResponseDto(
                worker.getId(),
                worker.getName(),
                worker.getHoursPerDay(),
                worker.getSkills(),
                email,
                email != null && !email.isBlank()
        );
    }
}
