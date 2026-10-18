package com.example.taskassignment.service;

import com.example.taskassignment.domain.Assignment;
import com.example.taskassignment.domain.AssignmentDay;
import com.example.taskassignment.domain.Task;
import com.example.taskassignment.domain.Worker;
import com.example.taskassignment.dto.AssignmentResponseDto;
import com.example.taskassignment.dto.AutoAssignmentResultDto;
import com.example.taskassignment.dto.TaskSummaryDto;
import com.example.taskassignment.dto.UnassignedTaskDto;
import com.example.taskassignment.repository.AssignmentDayRepository;
import com.example.taskassignment.repository.AssignmentRepository;
import com.example.taskassignment.repository.TaskRepository;
import com.example.taskassignment.repository.WorkerDailyLoadProjection;
import com.example.taskassignment.repository.WorkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AutoAssignmentService {

    private final TaskRepository taskRepository;
    private final WorkerRepository workerRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentDayRepository assignmentDayRepository;
    private final CalendarService calendarService;
    private final Clock clock;

    public AutoAssignmentService(TaskRepository taskRepository,
                                 WorkerRepository workerRepository,
                                 AssignmentRepository assignmentRepository,
                                 AssignmentDayRepository assignmentDayRepository,
                                 CalendarService calendarService,
                                 Clock clock) {
        this.taskRepository = taskRepository;
        this.workerRepository = workerRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentDayRepository = assignmentDayRepository;
        this.calendarService = calendarService;
        this.clock = clock;
    }

    public AutoAssignmentResultDto autoAssign(LocalDate requestedStartDate) {
        LocalDate startDate = requestedStartDate != null ? requestedStartDate : LocalDate.now(clock);

        // 1. Load all tasks with status NOT_DONE and no existing assignment
        List<Task> unassignedTasks = taskRepository.findUnassignedNotDoneTasks();
        if (unassignedTasks.isEmpty()) {
            return new AutoAssignmentResultDto(List.of(), List.of());
        }

        // 2. Load all workers once
        List<Worker> workers = workerRepository.findAll();
        if (workers.isEmpty()) {
            List<UnassignedTaskDto> unassigned = unassignedTasks.stream()
                    .map(task -> new UnassignedTaskDto(
                            new TaskSummaryDto(task.getId(), task.getName()),
                            "There are no workers"
                    ))
                    .toList();
            return new AutoAssignmentResultDto(List.of(), unassigned);
        }

        // Load existing daily load and total allocated hours for each worker into memory ONCE
        Map<Long, Map<LocalDate, Integer>> workerDailyLoads = new HashMap<>();
        Map<Long, Integer> workerTotalHours = new HashMap<>();

        for (Worker worker : workers) {
            List<WorkerDailyLoadProjection> schedule = assignmentDayRepository.findWorkerSchedule(worker.getId());
            Map<LocalDate, Integer> dailyMap = new HashMap<>();
            int total = 0;
            for (WorkerDailyLoadProjection entry : schedule) {
                dailyMap.put(entry.getDate(), entry.getHours());
                total += entry.getHours();
            }
            workerDailyLoads.put(worker.getId(), dailyMap);
            workerTotalHours.put(worker.getId(), total);
        }

        Set<String> allWorkersSkills = workers.stream()
                .flatMap(w -> w.getSkills().stream())
                .collect(Collectors.toSet());

        // 3. For each task, compute its eligible workers
        List<TaskEligibility> eligibilityList = new ArrayList<>();
        for (Task task : unassignedTasks) {
            List<Worker> eligible = workers.stream()
                    .filter(w -> SkillUtils.hasAllSkills(w.getSkills(), task.getSkills()))
                    .toList();
            eligibilityList.add(new TaskEligibility(task, eligible));
        }

        // 4. Sort tasks: fewest eligible workers first, then highest effortHours, then lowest id
        eligibilityList.sort(Comparator
                .<TaskEligibility>comparingInt(te -> te.eligibleWorkers().size())
                .thenComparing(Comparator.comparingInt((TaskEligibility te) -> te.task().getEffortHours()).reversed())
                .thenComparing(te -> te.task().getId()));

        List<Assignment> assignmentsToPersist = new ArrayList<>();
        List<UnassignedTaskDto> unassignedList = new ArrayList<>();

        // 5. Greedily simulate and choose the best eligible worker for each task
        for (TaskEligibility item : eligibilityList) {
            Task task = item.task();
            List<Worker> eligible = item.eligibleWorkers();

            if (eligible.isEmpty()) {
                String reason = determineUnassignedReason(task, allWorkersSkills);
                unassignedList.add(new UnassignedTaskDto(
                        new TaskSummaryDto(task.getId(), task.getName()),
                        reason
                ));
                continue;
            }

            Candidate bestCandidate = null;
            for (Worker worker : eligible) {
                Map<LocalDate, Integer> inMemoryLoad = workerDailyLoads.get(worker.getId());
                Placement placement = calendarService.plan(startDate, task.getEffortHours(), worker.getHoursPerDay(), inMemoryLoad);
                int totalHours = workerTotalHours.get(worker.getId());

                Candidate candidate = new Candidate(worker, placement, totalHours);
                if (bestCandidate == null || isBetterCandidate(candidate, bestCandidate)) {
                    bestCandidate = candidate;
                }
            }

            // Create assignment with days
            Worker chosenWorker = bestCandidate.worker();
            Placement chosenPlacement = bestCandidate.placement();

            Assignment assignment = new Assignment(
                    task,
                    chosenWorker,
                    chosenPlacement.effectiveStartDate(),
                    chosenPlacement.endDate()
            );
            for (DailyAllocation allocation : chosenPlacement.allocations()) {
                assignment.addAssignmentDay(new AssignmentDay(assignment, allocation.date(), allocation.hours()));

                // Update in-memory load for subsequent tasks in this run
                workerDailyLoads.get(chosenWorker.getId()).merge(allocation.date(), allocation.hours(), Integer::sum);
                workerTotalHours.put(chosenWorker.getId(), workerTotalHours.get(chosenWorker.getId()) + allocation.hours());
            }

            assignmentsToPersist.add(assignment);
        }

        // 6. Persist in a single transaction
        List<Assignment> saved = assignmentRepository.saveAll(assignmentsToPersist);
        List<AssignmentResponseDto> assignedResponses = saved.stream()
                .map(AssignmentResponseDto::fromEntity)
                .toList();

        return new AutoAssignmentResultDto(assignedResponses, unassignedList);
    }

    private boolean isBetterCandidate(Candidate candidate, Candidate currentBest) {
        // Priority 1: Earliest end date
        int endDateCmp = candidate.placement().endDate().compareTo(currentBest.placement().endDate());
        if (endDateCmp != 0) {
            return endDateCmp < 0;
        }

        // Priority 2: Lowest total allocated hours
        int hoursCmp = Integer.compare(candidate.totalHours(), currentBest.totalHours());
        if (hoursCmp != 0) {
            return hoursCmp < 0;
        }

        // Priority 3: Lowest worker id
        return candidate.worker().getId().compareTo(currentBest.worker().getId()) < 0;
    }

    private String determineUnassignedReason(Task task, Set<String> allWorkersSkills) {
        List<String> missingFromAll = SkillUtils.findMissingSkills(allWorkersSkills, task.getSkills());

        if (!missingFromAll.isEmpty()) {
            return "No worker has skills: " + missingFromAll;
        }

        List<String> allRequired = task.getSkills().stream().sorted().toList();
        return "No single worker has all required skills: " + allRequired;
    }

    private record TaskEligibility(Task task, List<Worker> eligibleWorkers) {
    }

    private record Candidate(Worker worker, Placement placement, int totalHours) {
    }
}

