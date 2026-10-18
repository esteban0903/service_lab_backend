package com.example.taskassignment.service;

import com.example.taskassignment.domain.Assignment;
import com.example.taskassignment.domain.AssignmentDay;
import com.example.taskassignment.domain.Task;
import com.example.taskassignment.domain.TaskStatus;
import com.example.taskassignment.domain.Worker;
import com.example.taskassignment.dto.common.SeedSummaryDto;
import com.example.taskassignment.exception.ResourceNotFoundException;
import com.example.taskassignment.domain.Role;
import com.example.taskassignment.domain.User;
import com.example.taskassignment.repository.AssignmentDayRepository;
import com.example.taskassignment.repository.AssignmentRepository;
import com.example.taskassignment.repository.TaskRepository;
import com.example.taskassignment.repository.WorkerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class SeedService {

    private final WorkerRepository workerRepository;
    private final TaskRepository taskRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentDayRepository assignmentDayRepository;
    private final com.example.taskassignment.repository.UserRepository userRepository;
    private final AuthService authService;
    private final CalendarService calendarService;
    private final Clock clock;
    private final boolean seedEnabled;

    public SeedService(WorkerRepository workerRepository,
                       TaskRepository taskRepository,
                       AssignmentRepository assignmentRepository,
                       AssignmentDayRepository assignmentDayRepository,
                       com.example.taskassignment.repository.UserRepository userRepository,
                       AuthService authService,
                       CalendarService calendarService,
                       Clock clock,
                       @Value("${app.seed.enabled:false}") boolean seedEnabled) {
        this.workerRepository = workerRepository;
        this.taskRepository = taskRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentDayRepository = assignmentDayRepository;
        this.userRepository = userRepository;
        this.authService = authService;
        this.calendarService = calendarService;
        this.clock = clock;
        this.seedEnabled = seedEnabled;
    }

    public SeedSummaryDto seed() {
        if (!seedEnabled) {
            throw new ResourceNotFoundException("Seed endpoint is disabled");
        }

        // Idempotent: delete existing assignments, tasks, workers, and only WORKER users (preserve ADMIN)
        assignmentDayRepository.deleteAll();
        assignmentRepository.deleteAll();
        taskRepository.deleteAll();
        List<User> existingWorkerUsers = userRepository.findByRole(Role.WORKER);
        userRepository.deleteAll(existingWorkerUsers);
        userRepository.flush();
        workerRepository.deleteAll();
        workerRepository.flush();

        // 1. Create 5 Workers
        Worker ana = new Worker("Ana", 8, Set.of("java", "spring", "sql"));
        Worker bruno = new Worker("Bruno", 6, Set.of("react", "javascript", "css"));
        Worker carla = new Worker("Carla", 4, Set.of("java", "react", "javascript"));
        Worker diego = new Worker("Diego", 8, Set.of("python", "sql", "docker"));
        Worker elena = new Worker("Elena", 6, Set.of("java", "docker", "devops"));

        List<Worker> savedWorkers = workerRepository.saveAll(List.of(ana, bruno, carla, diego, elena));
        workerRepository.flush();
        Worker savedAna = savedWorkers.get(0);
        Worker savedBruno = savedWorkers.get(1);
        Worker savedCarla = savedWorkers.get(2);
        Worker savedDiego = savedWorkers.get(3);
        Worker savedElena = savedWorkers.get(4);

        // Create a WORKER user for each demo worker
        authService.createWorkerUser(savedAna, "ana@demo.com", "Worker123!");
        authService.createWorkerUser(savedBruno, "bruno@demo.com", "Worker123!");
        authService.createWorkerUser(savedCarla, "carla@demo.com", "Worker123!");
        authService.createWorkerUser(savedDiego, "diego@demo.com", "Worker123!");
        authService.createWorkerUser(savedElena, "elena@demo.com", "Worker123!");

        // 2. Create 12 Tasks
        Task t1 = new Task("Database Schema Migration", 8, TaskStatus.NOT_DONE, Set.of("java", "sql"));
        Task t2 = new Task("Security Audit & Hardening", 16, TaskStatus.NOT_DONE, Set.of("java", "docker"));
        Task t3 = new Task("Initial Project Setup", 4, TaskStatus.DONE, Set.of("java"));
        Task t4 = new Task("Mobile Android Client", 12, TaskStatus.NOT_DONE, Set.of("kotlin"));
        Task t5 = new Task("Fullstack Analytics Dashboard", 10, TaskStatus.NOT_DONE, Set.of("python", "react"));
        Task t6 = new Task("Documentation & Architecture Diagrams", 6, TaskStatus.NOT_DONE, Set.of());
        Task t7 = new Task("Clean Up Code Debt", 8, TaskStatus.NOT_DONE, Set.of());
        Task t8 = new Task("Customer Portal Frontend", 24, TaskStatus.NOT_DONE, Set.of("react", "javascript"));
        Task t9 = new Task("Cloud Infrastructure CI/CD Pipeline", 20, TaskStatus.NOT_DONE, Set.of("docker", "devops"));
        Task t10 = new Task("Data Pipeline Automation", 12, TaskStatus.NOT_DONE, Set.of("python", "sql"));
        Task t11 = new Task("Spring Boot REST Optimization", 8, TaskStatus.NOT_DONE, Set.of("java", "spring"));
        Task t12 = new Task("Authentication & Role Management", 8, TaskStatus.NOT_DONE, Set.of("java"));

        List<Task> savedTasks = taskRepository.saveAll(List.of(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12));
        taskRepository.flush();
        Task savedT1 = savedTasks.get(0);
        Task savedT2 = savedTasks.get(1);

        // 3. Create 2 manual assignments relative to the Clock (next Monday)
        LocalDate today = LocalDate.now(clock);
        LocalDate nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        LocalDate nextThursday = nextMonday.plusDays(3); // Thursday of next week

        // Manual 1: T1 to Ana on next Monday (8h fits in Monday)
        Placement p1 = calendarService.plan(nextMonday, savedT1.getEffortHours(), savedAna.getHoursPerDay(), Collections.emptyMap());
        Assignment a1 = new Assignment(savedT1, savedAna, p1.effectiveStartDate(), p1.endDate());
        for (DailyAllocation alloc : p1.allocations()) {
            a1.addAssignmentDay(new AssignmentDay(a1, alloc.date(), alloc.hours()));
        }

        // Manual 2: T2 to Elena starting next Thursday (16h at 6h/day: Thu 6, Fri 6, Mon 4 -> crosses weekend)
        Placement p2 = calendarService.plan(nextThursday, savedT2.getEffortHours(), savedElena.getHoursPerDay(), Collections.emptyMap());
        Assignment a2 = new Assignment(savedT2, savedElena, p2.effectiveStartDate(), p2.endDate());
        for (DailyAllocation alloc : p2.allocations()) {
            a2.addAssignmentDay(new AssignmentDay(a2, alloc.date(), alloc.hours()));
        }

        assignmentRepository.saveAll(List.of(a1, a2));

        return new SeedSummaryDto(savedWorkers.size(), savedTasks.size(), 2, nextMonday);
    }
}


