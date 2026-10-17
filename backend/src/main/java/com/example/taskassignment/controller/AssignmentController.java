package com.example.taskassignment.controller;

import com.example.taskassignment.dto.AssignmentRequestDto;
import com.example.taskassignment.dto.AssignmentResponseDto;
import com.example.taskassignment.dto.AutoAssignmentRequestDto;
import com.example.taskassignment.dto.AutoAssignmentResultDto;
import com.example.taskassignment.service.AssignmentService;
import com.example.taskassignment.service.AutoAssignmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final AutoAssignmentService autoAssignmentService;

    public AssignmentController(AssignmentService assignmentService, AutoAssignmentService autoAssignmentService) {
        this.assignmentService = assignmentService;
        this.autoAssignmentService = autoAssignmentService;
    }

    @GetMapping
    public List<AssignmentResponseDto> getAllAssignments() {
        return assignmentService.getAll();
    }

    @PostMapping
    public ResponseEntity<AssignmentResponseDto> createAssignment(@Valid @RequestBody AssignmentRequestDto request) {
        AssignmentResponseDto created = assignmentService.create(
                request.taskId(),
                request.workerId(),
                request.startDate()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/auto")
    public AutoAssignmentResultDto autoAssign(@RequestBody(required = false) AutoAssignmentRequestDto request) {
        return autoAssignmentService.autoAssign(request != null ? request.startDate() : null);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssignment(@PathVariable Long id) {
        assignmentService.delete(id);
    }
}

