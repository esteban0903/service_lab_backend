package com.example.taskassignment.dto.assignment;

import com.example.taskassignment.dto.task.UnassignedTaskDto;

import java.util.List;

public record AutoAssignmentResultDto(
        List<AssignmentResponseDto> assigned,
        List<UnassignedTaskDto> unassigned
) {
}

