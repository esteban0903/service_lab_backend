package com.example.taskassignment.dto;

public record UnassignedTaskDto(
        TaskSummaryDto task,
        String reason
) {
}

