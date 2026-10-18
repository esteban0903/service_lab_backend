package com.example.taskassignment.service;

import com.example.taskassignment.domain.Task;
import com.example.taskassignment.domain.Worker;
import com.example.taskassignment.dto.skill.SkillCatalogItemDto;
import com.example.taskassignment.dto.skill.SkillCoverageItemDto;
import com.example.taskassignment.dto.task.TaskCoverageRequestDto;
import com.example.taskassignment.dto.task.TaskCoverageResponseDto;
import com.example.taskassignment.repository.TaskRepository;
import com.example.taskassignment.repository.WorkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SkillService {

    private final WorkerRepository workerRepository;
    private final TaskRepository taskRepository;

    public SkillService(WorkerRepository workerRepository, TaskRepository taskRepository) {
        this.workerRepository = workerRepository;
        this.taskRepository = taskRepository;
    }

    /**
     * Returns the distinct normalized skills across workers and tasks,
     * each with workerCount (workers who have it) and taskCount (tasks that require it),
     * sorted alphabetically by name.
     */
    public List<SkillCatalogItemDto> getSkillCatalog() {
        // Load all workers and tasks with their skills (using single fetch join query per entity)
        List<Worker> workers = workerRepository.findAllWithSkills();
        List<Task> tasks = taskRepository.findAllWithSkills();

        // Tally distinct worker IDs per normalized skill
        Map<String, Set<Long>> workersPerSkill = new HashMap<>();
        for (Worker worker : workers) {
            Set<String> normalizedSkills = SkillUtils.normalizeSkills(worker.getSkills());
            for (String skill : normalizedSkills) {
                workersPerSkill.computeIfAbsent(skill, k -> new HashSet<>()).add(worker.getId());
            }
        }

        // Tally distinct task IDs per normalized skill
        Map<String, Set<Long>> tasksPerSkill = new HashMap<>();
        for (Task task : tasks) {
            Set<String> normalizedSkills = SkillUtils.normalizeSkills(task.getSkills());
            for (String skill : normalizedSkills) {
                tasksPerSkill.computeIfAbsent(skill, k -> new HashSet<>()).add(task.getId());
            }
        }

        // Combine all distinct skill names
        Set<String> allSkills = new TreeSet<>();
        allSkills.addAll(workersPerSkill.keySet());
        allSkills.addAll(tasksPerSkill.keySet());

        return allSkills.stream()
                .map(skill -> new SkillCatalogItemDto(
                        skill,
                        workersPerSkill.getOrDefault(skill, Collections.emptySet()).size(),
                        tasksPerSkill.getOrDefault(skill, Collections.emptySet()).size()
                ))
                .toList();
    }

    /**
     * Evaluates worker coverage for a given set of skills.
     * Reuses SkillUtils.normalizeSkills and worker.getSkills().containsAll(requiredSkillSet).
     */
    public TaskCoverageResponseDto evaluateCoverage(TaskCoverageRequestDto request) {
        List<String> rawSkills = request != null ? request.skills() : null;
        Set<String> normalizedRequired = SkillUtils.normalizeSkills(rawSkills);

        List<Worker> workers = workerRepository.findAllWithSkills();

        // If skill list is empty, task has no requirements: can go to anyone
        if (normalizedRequired.isEmpty()) {
            return new TaskCoverageResponseDto(
                    workers.size(),
                    List.of(),
                    List.of(),
                    null
            );
        }

        // Pre-normalize worker skills in memory (avoiding redundant calls)
        List<Set<String>> workerSkillSets = workers.stream()
                .map(w -> SkillUtils.normalizeSkills(w.getSkills()))
                .toList();

        // 1. Per-skill worker count
        List<SkillCoverageItemDto> perSkill = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();

        for (String reqSkill : normalizedRequired) {
            long count = workerSkillSets.stream()
                    .filter(wSkills -> wSkills.contains(reqSkill))
                    .count();
            perSkill.add(new SkillCoverageItemDto(reqSkill, count));
            if (count == 0) {
                missingSkills.add(reqSkill);
            }
        }

        // 2. Exact match: workers having ALL requested skills (reusing SkillUtils.hasAllSkills)
        int coveredBy = (int) workerSkillSets.stream()
                .filter(wSkills -> SkillUtils.hasAllSkills(wSkills, normalizedRequired))
                .count();

        // 3. Warning wording distinguishing:
        // (a) "No worker has skills: [x]"
        // (b) "Each skill exists, but no single worker has all of them"
        String warning = null;
        if (coveredBy == 0) {
            if (!missingSkills.isEmpty()) {
                warning = "No worker has skills: " + missingSkills;
            } else {
                warning = "Each skill exists, but no single worker has all of them";
            }
        }

        return new TaskCoverageResponseDto(
                coveredBy,
                perSkill,
                missingSkills,
                warning
        );
    }
}
