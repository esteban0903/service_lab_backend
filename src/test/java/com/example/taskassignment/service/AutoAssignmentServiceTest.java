package com.example.taskassignment.service;

import com.example.taskassignment.domain.Assignment;
import com.example.taskassignment.domain.Task;
import com.example.taskassignment.domain.TaskStatus;
import com.example.taskassignment.domain.Worker;
import com.example.taskassignment.dto.AutoAssignmentResultDto;
import com.example.taskassignment.repository.AssignmentDayRepository;
import com.example.taskassignment.repository.AssignmentRepository;
import com.example.taskassignment.repository.TaskRepository;
import com.example.taskassignment.repository.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutoAssignmentService Unit Tests")
class AutoAssignmentServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AssignmentDayRepository assignmentDayRepository;

    private CalendarService calendarService;
    private Clock fixedClock;
    private AutoAssignmentService autoAssignmentService;

    // Fixed clock date: 2026-10-15 (Thursday)
    private static final LocalDate THURSDAY = LocalDate.of(2026, 10, 15);

    @BeforeEach
    void setUp() {
        calendarService = new CalendarService();
        fixedClock = Clock.fixed(Instant.parse("2026-10-15T09:00:00Z"), ZoneId.of("UTC"));
        autoAssignmentService = new AutoAssignmentService(
                taskRepository,
                workerRepository,
                assignmentRepository,
                assignmentDayRepository,
                calendarService,
                fixedClock
        );

        lenient().when(assignmentRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("1. Most constrained task is processed first so it is not starved by flexible tasks")
    void autoAssign_mostConstrainedProcessedFirst() {
        // Worker 1: can do Java only, 8h/day
        Worker w1 = new Worker("Java Specialist", 8, Set.of("java"));
        w1.setId(1L);

        // Worker 2: generalist, can do Java and Python, 8h/day
        Worker w2 = new Worker("Generalist", 8, Set.of("java", "python"));
        w2.setId(2L);

        when(workerRepository.findAll()).thenReturn(List.of(w1, w2));

        // Task A: flexible (Java), effort 8h -> can be done by W1 or W2
        Task taskA = new Task("Flexible Task", 8, TaskStatus.NOT_DONE, Set.of("java"));
        taskA.setId(10L);

        // Task B: constrained (Python), effort 8h -> can only be done by W2
        Task taskB = new Task("Constrained Task", 8, TaskStatus.NOT_DONE, Set.of("python"));
        taskB.setId(20L);

        // Provide in order taskA, taskB to verify sorting places taskB first
        when(taskRepository.findUnassignedNotDoneTasks()).thenReturn(List.of(taskA, taskB));
        when(assignmentDayRepository.findWorkerSchedule(1L)).thenReturn(Collections.emptyList());
        when(assignmentDayRepository.findWorkerSchedule(2L)).thenReturn(Collections.emptyList());

        AutoAssignmentResultDto result = autoAssignmentService.autoAssign(THURSDAY);

        assertThat(result.unassigned()).isEmpty();
        assertThat(result.assigned()).hasSize(2);

        // Constrained task B must be assigned to Generalist (W2)
        // Flexible task A must be assigned to Java Specialist (W1)
        assertThat(result.assigned())
                .anySatisfy(a -> {
                    assertThat(a.task().id()).isEqualTo(20L);
                    assertThat(a.worker().id()).isEqualTo(2L);
                })
                .anySatisfy(a -> {
                    assertThat(a.task().id()).isEqualTo(10L);
                    assertThat(a.worker().id()).isEqualTo(1L);
                });
    }

    @Test
    @DisplayName("2. Chooses the worker that finishes earliest, taking existing load into account")
    void autoAssign_choosesWorkerFinishingEarliest() {
        // W1: 4h/day, takes 2 days for 8h (ends Friday 2026-10-16)
        Worker w1 = new Worker("Part-timer", 4, Set.of("java"));
        w1.setId(1L);

        // W2: 8h/day, takes 1 day for 8h (ends Thursday 2026-10-15)
        Worker w2 = new Worker("Full-timer", 8, Set.of("java"));
        w2.setId(2L);

        when(workerRepository.findAll()).thenReturn(List.of(w1, w2));

        Task task = new Task("Single Task", 8, TaskStatus.NOT_DONE, Set.of("java"));
        task.setId(10L);

        when(taskRepository.findUnassignedNotDoneTasks()).thenReturn(List.of(task));
        when(assignmentDayRepository.findWorkerSchedule(1L)).thenReturn(Collections.emptyList());
        when(assignmentDayRepository.findWorkerSchedule(2L)).thenReturn(Collections.emptyList());

        AutoAssignmentResultDto result = autoAssignmentService.autoAssign(THURSDAY);

        assertThat(result.assigned()).hasSize(1);
        assertThat(result.assigned().getFirst().worker().id()).isEqualTo(2L);
        assertThat(result.assigned().getFirst().endDate()).isEqualTo(THURSDAY);
    }

    @Test
    @DisplayName("3. Tie-breaker: equal end dates -> lower total allocated hours; then lower worker id")
    void autoAssign_tieBreaker_lowestTotalHoursThenLowestId() {
        // W1: 8h/day, has 0h load
        Worker w1 = new Worker("Worker 1", 8, Set.of("java"));
        w1.setId(1L);

        // W2: 8h/day, has 0h load
        Worker w2 = new Worker("Worker 2", 8, Set.of("java"));
        w2.setId(2L);

        when(workerRepository.findAll()).thenReturn(List.of(w1, w2));

        // Task 1: 4h -> both would finish Thursday, both have 0h total load -> tie-breaker picks W1 (lower id)
        Task t1 = new Task("Task 1", 4, TaskStatus.NOT_DONE, Set.of("java"));
        t1.setId(10L);

        // Task 2: 4h -> both would finish Thursday, but W1 now has 4h load and W2 has 0h -> tie-breaker picks W2 (lower total hours)
        Task t2 = new Task("Task 2", 4, TaskStatus.NOT_DONE, Set.of("java"));
        t2.setId(20L);

        when(taskRepository.findUnassignedNotDoneTasks()).thenReturn(List.of(t1, t2));
        when(assignmentDayRepository.findWorkerSchedule(1L)).thenReturn(Collections.emptyList());
        when(assignmentDayRepository.findWorkerSchedule(2L)).thenReturn(Collections.emptyList());

        AutoAssignmentResultDto result = autoAssignmentService.autoAssign(THURSDAY);

        assertThat(result.assigned()).hasSize(2);
        assertThat(result.assigned().get(0).task().id()).isEqualTo(10L);
        assertThat(result.assigned().get(0).worker().id()).isEqualTo(1L);

        assertThat(result.assigned().get(1).task().id()).isEqualTo(20L);
        assertThat(result.assigned().get(1).worker().id()).isEqualTo(2L);
    }

    @Test
    @DisplayName("4. Spreads load across two equivalent workers over several tasks")
    void autoAssign_spreadsLoadAcrossEquivalentWorkers() {
        Worker w1 = new Worker("Worker A", 8, Set.of("java"));
        w1.setId(1L);

        Worker w2 = new Worker("Worker B", 8, Set.of("java"));
        w2.setId(2L);

        when(workerRepository.findAll()).thenReturn(List.of(w1, w2));

        Task t1 = new Task("Task 1", 8, TaskStatus.NOT_DONE, Set.of("java"));
        t1.setId(1L);
        Task t2 = new Task("Task 2", 8, TaskStatus.NOT_DONE, Set.of("java"));
        t2.setId(2L);
        Task t3 = new Task("Task 3", 8, TaskStatus.NOT_DONE, Set.of("java"));
        t3.setId(3L);
        Task t4 = new Task("Task 4", 8, TaskStatus.NOT_DONE, Set.of("java"));
        t4.setId(4L);

        when(taskRepository.findUnassignedNotDoneTasks()).thenReturn(List.of(t1, t2, t3, t4));
        when(assignmentDayRepository.findWorkerSchedule(1L)).thenReturn(Collections.emptyList());
        when(assignmentDayRepository.findWorkerSchedule(2L)).thenReturn(Collections.emptyList());

        AutoAssignmentResultDto result = autoAssignmentService.autoAssign(THURSDAY);

        assertThat(result.assigned()).hasSize(4);

        long w1Count = result.assigned().stream().filter(a -> a.worker().id().equals(1L)).count();
        long w2Count = result.assigned().stream().filter(a -> a.worker().id().equals(2L)).count();

        // Balanced 2 and 2
        assertThat(w1Count).isEqualTo(2);
        assertThat(w2Count).isEqualTo(2);
    }

    @Test
    @DisplayName("5. Impossible tasks reported with exact required reasons, while possible tasks are assigned")
    void autoAssign_impossibleTaskReasons() {
        // W1 has [java], W2 has [docker]
        Worker w1 = new Worker("Worker 1", 8, Set.of("java"));
        w1.setId(1L);
        Worker w2 = new Worker("Worker 2", 8, Set.of("docker"));
        w2.setId(2L);

        when(workerRepository.findAll()).thenReturn(List.of(w1, w2));

        // Task 1: Valid, can be done by W1
        Task tValid = new Task("Valid Task", 4, TaskStatus.NOT_DONE, Set.of("java"));
        tValid.setId(1L);

        // Task 2: Required skill [rust] exists in no worker
        Task tNoWorkerHasSkill = new Task("Rust Task", 4, TaskStatus.NOT_DONE, Set.of("rust"));
        tNoWorkerHasSkill.setId(2L);

        // Task 3: Both [java, docker] required -> each exists individually but no single worker has both
        Task tNoSingleWorker = new Task("Fullstack Task", 4, TaskStatus.NOT_DONE, Set.of("java", "docker"));
        tNoSingleWorker.setId(3L);

        when(taskRepository.findUnassignedNotDoneTasks()).thenReturn(List.of(tValid, tNoWorkerHasSkill, tNoSingleWorker));
        when(assignmentDayRepository.findWorkerSchedule(1L)).thenReturn(Collections.emptyList());
        when(assignmentDayRepository.findWorkerSchedule(2L)).thenReturn(Collections.emptyList());

        AutoAssignmentResultDto result = autoAssignmentService.autoAssign(THURSDAY);

        assertThat(result.assigned()).hasSize(1);
        assertThat(result.assigned().getFirst().task().id()).isEqualTo(1L);

        assertThat(result.unassigned()).hasSize(2);

        // Reason 1: No worker has skills
        assertThat(result.unassigned())
                .anySatisfy(u -> {
                    assertThat(u.task().id()).isEqualTo(2L);
                    assertThat(u.reason()).isEqualTo("No worker has skills: [rust]");
                });

        // Reason 2: No single worker has all required skills
        assertThat(result.unassigned())
                .anySatisfy(u -> {
                    assertThat(u.task().id()).isEqualTo(3L);
                    assertThat(u.reason()).isEqualTo("No single worker has all required skills: [docker, java]");
                });
    }

    @Test
    @DisplayName("5b. Impossible task reason when there are no workers in the system")
    void autoAssign_noWorkers_reportsReason() {
        when(workerRepository.findAll()).thenReturn(Collections.emptyList());

        Task task = new Task("Any Task", 4, TaskStatus.NOT_DONE, Set.of());
        task.setId(1L);
        when(taskRepository.findUnassignedNotDoneTasks()).thenReturn(List.of(task));

        AutoAssignmentResultDto result = autoAssignmentService.autoAssign(THURSDAY);

        assertThat(result.assigned()).isEmpty();
        assertThat(result.unassigned()).hasSize(1);
        assertThat(result.unassigned().getFirst().reason()).isEqualTo("There are no workers");
    }

    @Test
    @DisplayName("6. Empty tasks list returns empty result with no errors")
    void autoAssign_noTasks_returnsEmpty() {
        when(taskRepository.findUnassignedNotDoneTasks()).thenReturn(Collections.emptyList());

        AutoAssignmentResultDto result = autoAssignmentService.autoAssign(THURSDAY);

        assertThat(result.assigned()).isEmpty();
        assertThat(result.unassigned()).isEmpty();
    }

    @Test
    @DisplayName("7. Determinism: identical input data produces identical assignments")
    void autoAssign_deterministic() {
        Worker w1 = new Worker("Worker 1", 8, Set.of("java", "spring"));
        w1.setId(1L);
        Worker w2 = new Worker("Worker 2", 8, Set.of("java", "docker"));
        w2.setId(2L);

        when(workerRepository.findAll()).thenReturn(List.of(w1, w2));

        Task t1 = new Task("Task 1", 6, TaskStatus.NOT_DONE, Set.of("java"));
        t1.setId(10L);
        Task t2 = new Task("Task 2", 8, TaskStatus.NOT_DONE, Set.of("docker"));
        t2.setId(20L);

        when(taskRepository.findUnassignedNotDoneTasks()).thenReturn(List.of(t1, t2));
        when(assignmentDayRepository.findWorkerSchedule(1L)).thenReturn(Collections.emptyList());
        when(assignmentDayRepository.findWorkerSchedule(2L)).thenReturn(Collections.emptyList());

        AutoAssignmentResultDto run1 = autoAssignmentService.autoAssign(THURSDAY);
        AutoAssignmentResultDto run2 = autoAssignmentService.autoAssign(THURSDAY);

        assertThat(run1.assigned()).hasSameSizeAs(run2.assigned());
        for (int i = 0; i < run1.assigned().size(); i++) {
            assertThat(run1.assigned().get(i).task().id()).isEqualTo(run2.assigned().get(i).task().id());
            assertThat(run1.assigned().get(i).worker().id()).isEqualTo(run2.assigned().get(i).worker().id());
            assertThat(run1.assigned().get(i).startDate()).isEqualTo(run2.assigned().get(i).startDate());
            assertThat(run1.assigned().get(i).endDate()).isEqualTo(run2.assigned().get(i).endDate());
        }
    }

    @Test
    @DisplayName("8. Missing startDate uses injected Clock; Weekend startDate rolls to Monday")
    void autoAssign_missingStartDateUsesClock_weekendRollsToMonday() {
        // Clock is fixed to Thursday 2026-10-15
        Worker w = new Worker("Worker", 8, Set.of("java"));
        w.setId(1L);
        when(workerRepository.findAll()).thenReturn(List.of(w));

        Task t = new Task("Task", 4, TaskStatus.NOT_DONE, Set.of("java"));
        t.setId(10L);
        when(taskRepository.findUnassignedNotDoneTasks()).thenReturn(List.of(t));
        when(assignmentDayRepository.findWorkerSchedule(1L)).thenReturn(Collections.emptyList());

        // Call with null startDate -> should default to Clock date (2026-10-15)
        AutoAssignmentResultDto resultWithNullDate = autoAssignmentService.autoAssign(null);
        assertThat(resultWithNullDate.assigned().getFirst().startDate()).isEqualTo(LocalDate.of(2026, 10, 15));

        // Call with Saturday 2026-10-17 -> should roll to Monday 2026-10-19
        LocalDate saturday = LocalDate.of(2026, 10, 17);
        AutoAssignmentResultDto resultWithSaturday = autoAssignmentService.autoAssign(saturday);
        assertThat(resultWithSaturday.assigned().getFirst().startDate()).isEqualTo(LocalDate.of(2026, 10, 19));
    }

    @Test
    @DisplayName("9. Clock at 2026-10-17T23:30 America/Bogota is treated as Saturday (not UTC Sunday 2026-10-18)")
    void autoAssign_bogotaTimezone_usesSaturdayNotUtcSunday() {
        // 2026-10-17T23:30 in America/Bogota corresponds to 2026-10-18T04:30Z in UTC
        ZoneId bogotaZone = ZoneId.of("America/Bogota");
        ZonedDateTime bogotaDateTime = ZonedDateTime.of(2026, 10, 17, 23, 30, 0, 0, bogotaZone);
        Clock bogotaClock = Clock.fixed(bogotaDateTime.toInstant(), bogotaZone);

        // Verify clock LocalDate.now() is Saturday 2026-10-17, NOT Sunday 2026-10-18
        assertThat(LocalDate.now(bogotaClock)).isEqualTo(LocalDate.of(2026, 10, 17));
        assertThat(LocalDate.now(Clock.fixed(bogotaDateTime.toInstant(), ZoneId.of("UTC")))).isEqualTo(LocalDate.of(2026, 10, 18));

        AutoAssignmentService serviceWithBogota = new AutoAssignmentService(
                taskRepository,
                workerRepository,
                assignmentRepository,
                assignmentDayRepository,
                calendarService,
                bogotaClock
        );

        Worker w = new Worker("Worker", 8, Set.of("java"));
        w.setId(1L);
        when(workerRepository.findAll()).thenReturn(List.of(w));

        Task t = new Task("Task", 4, TaskStatus.NOT_DONE, Set.of("java"));
        t.setId(10L);
        when(taskRepository.findUnassignedNotDoneTasks()).thenReturn(List.of(t));
        when(assignmentDayRepository.findWorkerSchedule(1L)).thenReturn(Collections.emptyList());

        // When startDate is null, it resolves to Saturday 2026-10-17 (Bogota time), which rolls to Monday 2026-10-19
        AutoAssignmentResultDto result = serviceWithBogota.autoAssign(null);
        assertThat(result.assigned().getFirst().startDate()).isEqualTo(LocalDate.of(2026, 10, 19));
    }
}
