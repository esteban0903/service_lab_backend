package com.example.taskassignment.service;

import com.example.taskassignment.domain.Task;
import com.example.taskassignment.domain.TaskStatus;
import com.example.taskassignment.dto.TaskRequestDto;
import com.example.taskassignment.dto.TaskResponseDto;
import com.example.taskassignment.exception.ResourceNotFoundException;
import com.example.taskassignment.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDto> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(TaskResponseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponseDto getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return TaskResponseDto.fromEntity(task);
    }

    public TaskResponseDto createTask(TaskRequestDto dto) {
        Set<String> normalizedSkills = SkillUtils.normalizeSkills(dto.skills());
        TaskStatus status = dto.status() != null ? dto.status() : TaskStatus.NOT_DONE;

        Task task = new Task(
                dto.name().trim(),
                dto.effortHours(),
                status,
                normalizedSkills
        );

        Task saved = taskRepository.save(task);
        return TaskResponseDto.fromEntity(saved);
    }

    public TaskResponseDto updateTask(Long id, TaskRequestDto dto) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        // TODO Phase 2: Check if task is assigned. Editing effortHours or skills of an assigned task must throw ConflictException (409).
        // Editing name or status is permitted even when assigned.

        task.setName(dto.name().trim());
        task.setEffortHours(dto.effortHours());
        if (dto.status() != null) {
            task.setStatus(dto.status());
        }
        task.setSkills(SkillUtils.normalizeSkills(dto.skills()));

        Task updated = taskRepository.save(task);
        return TaskResponseDto.fromEntity(updated);
    }

    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        // TODO Phase 2: Deleting a task cascades to its assignment and AssignmentDay records.

        taskRepository.delete(task);
    }
}

