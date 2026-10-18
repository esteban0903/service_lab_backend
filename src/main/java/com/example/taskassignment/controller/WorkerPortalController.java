package com.example.taskassignment.controller;

import com.example.taskassignment.dto.assignment.AssignmentResponseDto;
import com.example.taskassignment.dto.task.TaskResponseDto;
import com.example.taskassignment.dto.task.TaskStatusUpdateRequestDto;
import com.example.taskassignment.exception.BadRequestException;
import com.example.taskassignment.service.AssignmentService;
import com.example.taskassignment.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/me")
public class WorkerPortalController {

    private final AssignmentService assignmentService;
    private final TaskService taskService;

    public WorkerPortalController(AssignmentService assignmentService, TaskService taskService) {
        this.assignmentService = assignmentService;
        this.taskService = taskService;
    }

    @GetMapping("/assignments")
    public List<AssignmentResponseDto> getMyAssignments(@AuthenticationPrincipal Jwt jwt) {
        Long workerId = extractWorkerId(jwt);
        return assignmentService.getAssignmentsByWorkerId(workerId);
    }

    @PatchMapping("/tasks/{taskId}/status")
    public TaskResponseDto updateMyTaskStatus(
            @PathVariable Long taskId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TaskStatusUpdateRequestDto request
    ) {
        Long workerId = extractWorkerId(jwt);
        return taskService.updateTaskStatusForWorker(taskId, workerId, request.status());
    }

    private Long extractWorkerId(Jwt jwt) {
        Object workerIdClaim = jwt.getClaim("workerId");
        if (workerIdClaim == null) {
            throw new AccessDeniedException("No worker associated with this account");
        }
        if (workerIdClaim instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(workerIdClaim.toString());
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid worker ID in token claim");
        }
    }
}

