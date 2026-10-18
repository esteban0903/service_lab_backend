package com.example.taskassignment.service;

import com.example.taskassignment.domain.Assignment;
import com.example.taskassignment.domain.Task;
import com.example.taskassignment.domain.TaskStatus;
import com.example.taskassignment.domain.Worker;
import com.example.taskassignment.dto.assignment.AssignmentResponseDto;
import com.example.taskassignment.exception.BadRequestException;
import com.example.taskassignment.exception.ConflictException;
import com.example.taskassignment.exception.IncompatibleSkillsException;
import com.example.taskassignment.exception.ResourceNotFoundException;
import com.example.taskassignment.repository.AssignmentDayRepository;
import com.example.taskassignment.repository.AssignmentRepository;
import com.example.taskassignment.repository.TaskRepository;
import com.example.taskassignment.repository.WorkerDailyLoadProjection;
import com.example.taskassignment.repository.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssignmentService Unit Tests")
class AssignmentServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AssignmentDayRepository assignmentDayRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private CalendarService calendarService;

    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-10-15T00:00:00Z"), ZoneId.of("UTC"));

    @InjectMocks
    private AssignmentService assignmentService;

    private Task task;
    private Worker worker;

    @BeforeEach
    void setUp() {
        task = new Task("Backend Task", 8, TaskStatus.NOT_DONE, Set.of("java", "sql"));
        task.setId(10L);

        worker = new Worker("Alice Dev", 8, Set.of("java", "sql", "docker"));
        worker.setId(20L);
    }

    @Test
    @DisplayName("create should succeed when worker has all task skills and task is valid")
    void create_success() {
        LocalDate startDate = LocalDate.of(2026, 10, 15); // Thursday

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(workerRepository.findById(20L)).thenReturn(Optional.of(worker));
        when(assignmentRepository.existsByTaskId(10L)).thenReturn(false);
        when(assignmentDayRepository.findWorkerDailyLoadFromDate(20L, startDate)).thenReturn(Collections.emptyList());

        Placement mockPlacement = new Placement(
                startDate,
                startDate,
                List.of(new DailyAllocation(startDate, 8))
        );
        when(calendarService.plan(eq(startDate), eq(8), eq(8), any())).thenReturn(mockPlacement);

        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> {
            Assignment a = invocation.getArgument(0);
            a.setId(100L);
            return a;
        });

        AssignmentResponseDto response = assignmentService.create(10L, 20L, startDate);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.task().id()).isEqualTo(10L);
        assertThat(response.worker().id()).isEqualTo(20L);
        assertThat(response.startDate()).isEqualTo(startDate);
        assertThat(response.endDate()).isEqualTo(startDate);
        assertThat(response.allocations()).hasSize(1);
    }

    @Test
    @DisplayName("create should succeed when task has no required skills for any worker")
    void create_taskWithNoSkills_success() {
        task.setSkills(Collections.emptySet());
        worker.setSkills(Collections.emptySet());
        LocalDate startDate = LocalDate.of(2026, 10, 15);

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(workerRepository.findById(20L)).thenReturn(Optional.of(worker));
        when(assignmentRepository.existsByTaskId(10L)).thenReturn(false);
        when(assignmentDayRepository.findWorkerDailyLoadFromDate(20L, startDate)).thenReturn(Collections.emptyList());

        Placement mockPlacement = new Placement(startDate, startDate, List.of(new DailyAllocation(startDate, 8)));
        when(calendarService.plan(eq(startDate), eq(8), eq(8), any())).thenReturn(mockPlacement);
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(i -> {
            Assignment a = i.getArgument(0);
            a.setId(101L);
            return a;
        });

        AssignmentResponseDto response = assignmentService.create(10L, 20L, startDate);

        assertThat(response.id()).isEqualTo(101L);
    }

    @Test
    @DisplayName("create should throw IllegalArgumentException (400) when startDate is null")
    void create_nullStartDate_throwsException() {
        assertThatThrownBy(() -> assignmentService.create(10L, 20L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start date is required");
    }

    @Test
    @DisplayName("create should throw ResourceNotFoundException (404) when task does not exist")
    void create_taskNotFound_throwsException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.create(99L, 20L, LocalDate.of(2026, 10, 15)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task not found with id: 99");
    }

    @Test
    @DisplayName("create should throw ResourceNotFoundException (404) when worker does not exist")
    void create_workerNotFound_throwsException() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(workerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.create(10L, 99L, LocalDate.of(2026, 10, 15)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Worker not found with id: 99");
    }

    @Test
    @DisplayName("create should throw ConflictException (409) when task is DONE")
    void create_taskDone_throwsConflict() {
        task.setStatus(TaskStatus.DONE);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(workerRepository.findById(20L)).thenReturn(Optional.of(worker));

        assertThatThrownBy(() -> assignmentService.create(10L, 20L, LocalDate.of(2026, 10, 15)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot assign a task that is marked as DONE");
    }

    @Test
    @DisplayName("create should throw ConflictException (409) when task is already assigned")
    void create_taskAlreadyAssigned_throwsConflict() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(workerRepository.findById(20L)).thenReturn(Optional.of(worker));
        when(assignmentRepository.existsByTaskId(10L)).thenReturn(true);

        assertThatThrownBy(() -> assignmentService.create(10L, 20L, LocalDate.of(2026, 10, 15)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Task is already assigned");
    }

    @Test
    @DisplayName("create should throw IncompatibleSkillsException (422) listing missing skills when worker lacks required skills")
    void create_missingSkills_throwsUnprocessableEntity() {
        task.setSkills(Set.of("java", "kotlin", "aws"));
        worker.setSkills(Set.of("java")); // Missing: aws, kotlin

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(workerRepository.findById(20L)).thenReturn(Optional.of(worker));

        assertThatThrownBy(() -> assignmentService.create(10L, 20L, LocalDate.of(2026, 10, 15)))
                .isInstanceOf(IncompatibleSkillsException.class)
                .hasMessageContaining("Worker is missing required skills: [aws, kotlin]");
    }

    @Test
    @DisplayName("delete should remove assignment when found")
    void delete_success() {
        Assignment assignment = new Assignment(task, worker, LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 15));
        assignment.setId(100L);
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));

        assignmentService.delete(100L);

        verify(assignmentRepository).delete(assignment);
    }

    @Test
    @DisplayName("delete should throw ResourceNotFoundException (404) when assignment not found")
    void delete_notFound_throwsException() {
        when(assignmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Assignment not found with id: 999");
    }

    @Test
    @DisplayName("getWorkerSchedule should return schedule map when worker exists")
    void getWorkerSchedule_success() {
        when(workerRepository.existsById(20L)).thenReturn(true);

        LocalDate d1 = LocalDate.of(2026, 10, 15);
        LocalDate d2 = LocalDate.of(2026, 10, 16);

        WorkerDailyLoadProjection p1 = new WorkerDailyLoadProjection() {
            public LocalDate getDate() { return d1; }
            public Integer getHours() { return 4; }
        };
        WorkerDailyLoadProjection p2 = new WorkerDailyLoadProjection() {
            public LocalDate getDate() { return d2; }
            public Integer getHours() { return 6; }
        };

        when(assignmentDayRepository.findWorkerSchedule(20L)).thenReturn(List.of(p1, p2));

        Map<LocalDate, Integer> schedule = assignmentService.getWorkerSchedule(20L);

        assertThat(schedule).containsEntry(d1, 4);
        assertThat(schedule).containsEntry(d2, 6);
    }

    @Test
    @DisplayName("getWorkerSchedule should throw ResourceNotFoundException (404) when worker not found")
    void getWorkerSchedule_notFound_throwsException() {
        when(workerRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> assignmentService.getWorkerSchedule(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Worker not found with id: 99");
    }

    @Test
    @DisplayName("create should throw BadRequestException when startDate is in the past")
    void create_startDateInPast_throwsBadRequest() {
        LocalDate pastDate = LocalDate.of(2026, 10, 14);

        assertThatThrownBy(() -> assignmentService.create(10L, 20L, pastDate))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Start date cannot be in the past");
    }
}

