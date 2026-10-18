package com.example.taskassignment.dto.assignment;

import com.example.taskassignment.domain.Assignment;
import com.example.taskassignment.dto.task.TaskSummaryDto;
import com.example.taskassignment.dto.worker.WorkerSummaryDto;

import java.time.LocalDate;
import java.util.List;

public record AssignmentResponseDto(
        Long id,
        TaskSummaryDto task,
        WorkerSummaryDto worker,
        LocalDate startDate,
        LocalDate endDate,
        List<DailyAllocationDto> allocations
) {
    public static AssignmentResponseDto fromEntity(Assignment assignment) {
        List<DailyAllocationDto> allocations = assignment.getAssignmentDays().stream()
                .map(day -> new DailyAllocationDto(day.getDate(), day.getHours()))
                .toList();

        return new AssignmentResponseDto(
                assignment.getId(),
                new TaskSummaryDto(assignment.getTask().getId(), assignment.getTask().getName()),
                new WorkerSummaryDto(assignment.getWorker().getId(), assignment.getWorker().getName()),
                assignment.getStartDate(),
                assignment.getEndDate(),
                allocations
        );
    }
}

