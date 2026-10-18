package com.example.taskassignment.service;

import com.example.taskassignment.domain.Assignment;
import com.example.taskassignment.domain.AssignmentDay;
import com.example.taskassignment.domain.Task;
import com.example.taskassignment.domain.TaskStatus;
import com.example.taskassignment.domain.Worker;
import com.example.taskassignment.dto.AssignmentResponseDto;
import com.example.taskassignment.exception.ConflictException;
import com.example.taskassignment.exception.IncompatibleSkillsException;
import com.example.taskassignment.exception.ResourceNotFoundException;
import com.example.taskassignment.repository.AssignmentDayRepository;
import com.example.taskassignment.repository.AssignmentRepository;
import com.example.taskassignment.repository.TaskRepository;
import com.example.taskassignment.repository.WorkerDailyLoadProjection;
import com.example.taskassignment.repository.WorkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentDayRepository assignmentDayRepository;
    private final TaskRepository taskRepository;
    private final WorkerRepository workerRepository;
    private final CalendarService calendarService;

    public AssignmentService(AssignmentRepository assignmentRepository,
                             AssignmentDayRepository assignmentDayRepository,
                             TaskRepository taskRepository,
                             WorkerRepository workerRepository,
                             CalendarService calendarService) {
        this.assignmentRepository = assignmentRepository;
        this.assignmentDayRepository = assignmentDayRepository;
        this.taskRepository = taskRepository;
        this.workerRepository = workerRepository;
        this.calendarService = calendarService;
    }

    public AssignmentResponseDto create(Long taskId, Long workerId, LocalDate startDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Start date is required");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with id: " + workerId));

        if (task.getStatus() == TaskStatus.DONE) {
            throw new ConflictException("Cannot assign a task that is marked as DONE");
        }

        if (assignmentRepository.existsByTaskId(taskId)) {
            throw new ConflictException("Task is already assigned");
        }

        if (!SkillUtils.hasAllSkills(worker.getSkills(), task.getSkills())) {
            List<String> missingSkills = SkillUtils.findMissingSkills(worker.getSkills(), task.getSkills());
            throw new IncompatibleSkillsException("Worker is missing required skills: " + missingSkills);
        }

        List<WorkerDailyLoadProjection> existingLoads = assignmentDayRepository.findWorkerDailyLoadFromDate(workerId, startDate);
        Map<LocalDate, Integer> loadMap = existingLoads.stream()
                .collect(Collectors.toMap(WorkerDailyLoadProjection::getDate, WorkerDailyLoadProjection::getHours));

        Placement placement = calendarService.plan(startDate, task.getEffortHours(), worker.getHoursPerDay(), loadMap);

        Assignment assignment = new Assignment(task, worker, placement.effectiveStartDate(), placement.endDate());
        for (DailyAllocation allocation : placement.allocations()) {
            assignment.addAssignmentDay(new AssignmentDay(assignment, allocation.date(), allocation.hours()));
        }

        Assignment saved = assignmentRepository.save(assignment);
        return AssignmentResponseDto.fromEntity(saved);
    }

    public void delete(Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));

        assignmentRepository.delete(assignment);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponseDto> getAll() {
        return assignmentRepository.findAllWithDetails().stream()
                .map(AssignmentResponseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<LocalDate, Integer> getWorkerSchedule(Long workerId) {
        if (!workerRepository.existsById(workerId)) {
            throw new ResourceNotFoundException("Worker not found with id: " + workerId);
        }

        List<WorkerDailyLoadProjection> scheduleEntries = assignmentDayRepository.findWorkerSchedule(workerId);
        Map<LocalDate, Integer> scheduleMap = new LinkedHashMap<>();
        for (WorkerDailyLoadProjection entry : scheduleEntries) {
            scheduleMap.put(entry.getDate(), entry.getHours());
        }
        return scheduleMap;
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponseDto> getAssignmentsByWorkerId(Long workerId) {
        return assignmentRepository.findByWorkerIdWithDetails(workerId).stream()
                .map(AssignmentResponseDto::fromEntity)
                .toList();
    }
}

