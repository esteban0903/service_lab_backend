package com.example.taskassignment.controller;

import com.example.taskassignment.dto.worker.WorkerCredentialsRequestDto;
import com.example.taskassignment.dto.worker.WorkerRequestDto;
import com.example.taskassignment.dto.worker.WorkerResponseDto;
import com.example.taskassignment.service.AssignmentService;
import com.example.taskassignment.service.WorkerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workers")
public class WorkerController {

    private final WorkerService workerService;
    private final AssignmentService assignmentService;

    public WorkerController(WorkerService workerService, AssignmentService assignmentService) {
        this.workerService = workerService;
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public List<WorkerResponseDto> getAllWorkers() {
        return workerService.getAllWorkers();
    }

    @GetMapping("/{id}")
    public WorkerResponseDto getWorkerById(@PathVariable Long id) {
        return workerService.getWorkerById(id);
    }

    @GetMapping("/{id}/schedule")
    public Map<LocalDate, Integer> getWorkerSchedule(@PathVariable Long id) {
        return assignmentService.getWorkerSchedule(id);
    }

    @PostMapping
    public ResponseEntity<WorkerResponseDto> createWorker(@Valid @RequestBody WorkerRequestDto dto) {
        WorkerResponseDto created = workerService.createWorker(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public WorkerResponseDto updateWorker(@PathVariable Long id, @Valid @RequestBody WorkerRequestDto dto) {
        return workerService.updateWorker(id, dto);
    }

    @PutMapping("/{id}/credentials")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateWorkerCredentials(@PathVariable Long id, @Valid @RequestBody WorkerCredentialsRequestDto dto) {
        workerService.updateWorkerCredentials(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorker(@PathVariable Long id) {
        workerService.deleteWorker(id);
    }
}

