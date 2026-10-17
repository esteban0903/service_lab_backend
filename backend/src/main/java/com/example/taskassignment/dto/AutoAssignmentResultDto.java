package com.example.taskassignment.dto;

import java.util.List;

public record AutoAssignmentResultDto(
        List<AssignmentResponseDto> assigned,
        List<UnassignedTaskDto> unassigned
) {
}

