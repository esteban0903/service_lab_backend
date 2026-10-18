package com.example.taskassignment.dto.task;

public record UnassignedTaskDto(
        TaskSummaryDto task,
        String reason
) {
}

