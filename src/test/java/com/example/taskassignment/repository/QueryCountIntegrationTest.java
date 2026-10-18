package com.example.taskassignment.repository;

import com.example.taskassignment.domain.Assignment;
import com.example.taskassignment.domain.AssignmentDay;
import com.example.taskassignment.domain.Task;
import com.example.taskassignment.domain.TaskStatus;
import com.example.taskassignment.domain.Worker;
import com.example.taskassignment.model.Role;
import com.example.taskassignment.model.User;
import com.example.taskassignment.service.AssignmentService;
import com.example.taskassignment.service.TaskService;
import com.example.taskassignment.service.WorkerService;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@DisplayName("Query Count & N+1 Prevention Tests")
class QueryCountIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentDayRepository assignmentDayRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private WorkerService workerService;

    private Statistics statistics;
    private Worker worker1;
    private Worker worker2;

    @BeforeEach
    void setUp() {
        Session session = entityManager.unwrap(Session.class);
        statistics = session.getSessionFactory().getStatistics();
        statistics.setStatisticsEnabled(true);

        // Populate multiple workers, tasks and assignments
        worker1 = workerRepository.save(new Worker("Alice", 8, Set.of("java", "spring")));
        worker2 = workerRepository.save(new Worker("Bob", 8, Set.of("react", "css")));

        userRepository.save(new User("alice@demo.com", passwordEncoder.encode("Pass123!"), Role.WORKER, worker1));
        userRepository.save(new User("bob@demo.com", passwordEncoder.encode("Pass123!"), Role.WORKER, worker2));

        Task task1 = taskRepository.save(new Task("Task 1", 8, TaskStatus.NOT_DONE, Set.of("java")));
        Task task2 = taskRepository.save(new Task("Task 2", 8, TaskStatus.NOT_DONE, Set.of("react")));

        Assignment a1 = new Assignment(task1, worker1, LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 19));
        a1.addAssignmentDay(new AssignmentDay(a1, LocalDate.of(2026, 10, 19), 8));
        assignmentRepository.save(a1);

        Assignment a2 = new Assignment(task2, worker2, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 20));
        a2.addAssignmentDay(new AssignmentDay(a2, LocalDate.of(2026, 10, 20), 8));
        assignmentRepository.save(a2);

        entityManager.flush();
        entityManager.clear();
        statistics.clear();
    }

    @Test
    @DisplayName("AssignmentService.getAll() executes at most 1 SQL query (fetch join) avoiding N+1")
    void getAllAssignments_executesSingleQuery() {
        statistics.clear();

        var result = assignmentService.getAll();

        assertThat(result).hasSize(2);
        // Verify all fields are loaded in DTO
        assertThat(result.get(0).allocations()).isNotEmpty();
        assertThat(result.get(0).worker().name()).isNotEmpty();
        assertThat(result.get(0).task().name()).isNotEmpty();

        // Exactly 1 SQL query for the whole list
        assertThat(statistics.getQueryExecutionCount() + statistics.getEntityLoadCount()).isGreaterThan(0);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("AssignmentService.getAssignmentsByWorkerId() executes at most 1 SQL query (fetch join)")
    void getAssignmentsByWorkerId_executesSingleQuery() {
        statistics.clear();

        var result = assignmentService.getAssignmentsByWorkerId(worker1.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).allocations()).isNotEmpty();
        assertThat(result.get(0).worker().name()).isEqualTo("Alice");

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("TaskService.getAllTasks() executes at most 1 SQL query (fetch join)")
    void getAllTasks_executesSingleQuery() {
        statistics.clear();

        var result = taskService.getAllTasks();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).skills()).isNotEmpty();

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("WorkerService.getAllWorkers() executes 2 SQL queries (1 fetch join workers+skills, 1 batch users)")
    void getAllWorkers_executesBoundedQueriesWithoutNPlusOne() {
        statistics.clear();

        var result = workerService.getAllWorkers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).skills()).isNotEmpty();
        assertThat(result.get(0).email()).isNotEmpty();

        // 1 query for workers+skills, 1 query for users in batch (findByWorkerIdIn) -> 2 queries total, never 2+N
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2L);
    }
}
