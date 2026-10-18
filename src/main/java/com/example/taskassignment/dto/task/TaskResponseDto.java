package com.example.taskassignment.dto.task;

import com.example.taskassignment.domain.Task;
import com.example.taskassignment.domain.TaskStatus;

import java.util.Set;

public record TaskResponseDto(
        Long id,
        String name,
        int effortHours,
        TaskStatus status,
        Set<String> skills
) {
    public static TaskResponseDto fromEntity(Task task) {
        return new TaskResponseDto(
                task.getId(),
                task.getName(),
                task.getEffortHours(),
                task.getStatus(),
                task.getSkills()
        );
    }
}

