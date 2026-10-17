package com.example.taskassignment.service;

import com.example.taskassignment.domain.Task;
import com.example.taskassignment.domain.TaskStatus;
import com.example.taskassignment.dto.TaskRequestDto;
import com.example.taskassignment.dto.TaskResponseDto;
import com.example.taskassignment.exception.ResourceNotFoundException;
import com.example.taskassignment.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import com.example.taskassignment.exception.ConflictException;
import com.example.taskassignment.repository.AssignmentRepository;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @InjectMocks
    private TaskService taskService;

    private Task sampleTask;

    @BeforeEach
    void setUp() {
        sampleTask = new Task("Implement API", 8, TaskStatus.NOT_DONE, Set.of("java", "spring"));
        sampleTask.setId(1L);
    }

    @Test
    @DisplayName("createTask should trim name, normalize skills, default status to NOT_DONE, and save")
    void createTask_shouldNormalizeAndSave() {
        TaskRequestDto request = new TaskRequestDto("  Build Backend  ", 12, null, Set.of(" Java ", "SPRING "));

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task toSave = invocation.getArgument(0);
            toSave.setId(10L);
            return toSave;
        });

        TaskResponseDto response = taskService.createTask(request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Build Backend");
        assertThat(response.effortHours()).isEqualTo(12);
        assertThat(response.status()).isEqualTo(TaskStatus.NOT_DONE);
        assertThat(response.skills()).containsExactlyInAnyOrder("java", "spring");

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        Task captured = captor.getValue();
        assertThat(captured.getName()).isEqualTo("Build Backend");
        assertThat(captured.getStatus()).isEqualTo(TaskStatus.NOT_DONE);
        assertThat(captured.getSkills()).containsExactlyInAnyOrder("java", "spring");
    }

    @Test
    @DisplayName("getTaskById should return TaskResponseDto when task exists")
    void getTaskById_shouldReturnTask() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));

        TaskResponseDto response = taskService.getTaskById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Implement API");
        assertThat(response.effortHours()).isEqualTo(8);
        assertThat(response.skills()).containsExactlyInAnyOrder("java", "spring");
    }

    @Test
    @DisplayName("getTaskById should throw ResourceNotFoundException when task does not exist")
    void getTaskById_notFound_shouldThrowException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task not found with id: 99");
    }

    @Test
    @DisplayName("getAllTasks should return all tasks mapped to DTOs")
    void getAllTasks_shouldReturnList() {
        when(taskRepository.findAll()).thenReturn(List.of(sampleTask));

        List<TaskResponseDto> all = taskService.getAllTasks();

        assertThat(all).hasSize(1);
        assertThat(all.getFirst().name()).isEqualTo("Implement API");
    }

    @Test
    @DisplayName("updateTask should update task fields and normalize skills")
    void updateTask_shouldUpdateFields() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskRequestDto updateRequest = new TaskRequestDto("Updated API", 16, TaskStatus.DONE, Set.of(" KOTLIN "));

        TaskResponseDto response = taskService.updateTask(1L, updateRequest);

        assertThat(response.name()).isEqualTo("Updated API");
        assertThat(response.effortHours()).isEqualTo(16);
        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
        assertThat(response.skills()).containsExactly("kotlin");
    }

    @Test
    @DisplayName("updateTask should throw ResourceNotFoundException when task does not exist")
    void updateTask_notFound_shouldThrowException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        TaskRequestDto updateRequest = new TaskRequestDto("Updated API", 16, TaskStatus.DONE, Set.of());

        assertThatThrownBy(() -> taskService.updateTask(99L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task not found with id: 99");
    }

    @Test
    @DisplayName("deleteTask should delete task and any existing assignment when it exists")
    void deleteTask_shouldDelete() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(assignmentRepository.findByTaskId(1L)).thenReturn(Optional.empty());

        taskService.deleteTask(1L);

        verify(taskRepository).delete(sampleTask);
    }

    @Test
    @DisplayName("deleteTask should throw ResourceNotFoundException when task does not exist")
    void deleteTask_notFound_shouldThrowException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.deleteTask(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task not found with id: 99");
    }

    @Test
    @DisplayName("SPEC Section 6: updateTask should throw ConflictException if task is assigned and effortHours changes")
    void updateTask_assignedEffortChanged_throwsConflict() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(assignmentRepository.existsByTaskId(1L)).thenReturn(true);

        TaskRequestDto dto = new TaskRequestDto("Implement API", 12, TaskStatus.NOT_DONE, Set.of("java", "spring"));

        assertThatThrownBy(() -> taskService.updateTask(1L, dto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot modify effortHours or skills of an assigned task");
    }

    @Test
    @DisplayName("SPEC Section 6: updateTask should throw ConflictException if task is assigned and skills change")
    void updateTask_assignedSkillsChanged_throwsConflict() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(assignmentRepository.existsByTaskId(1L)).thenReturn(true);

        TaskRequestDto dto = new TaskRequestDto("Implement API", 8, TaskStatus.NOT_DONE, Set.of("java", "spring", "docker"));

        assertThatThrownBy(() -> taskService.updateTask(1L, dto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot modify effortHours or skills of an assigned task");
    }

    @Test
    @DisplayName("SPEC Section 6: updateTask should succeed if task is assigned but only name or status changes")
    void updateTask_assignedOnlyNameOrStatusChanged_succeeds() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(assignmentRepository.existsByTaskId(1L)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        TaskRequestDto dto = new TaskRequestDto("New Task Name", 8, TaskStatus.DONE, Set.of("java", "spring"));

        TaskResponseDto response = taskService.updateTask(1L, dto);

        assertThat(response.name()).isEqualTo("New Task Name");
        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
    }
}

