package com.example.taskassignment.service;

import com.example.taskassignment.domain.Worker;
import com.example.taskassignment.dto.WorkerRequestDto;
import com.example.taskassignment.dto.WorkerResponseDto;
import com.example.taskassignment.exception.ResourceNotFoundException;
import com.example.taskassignment.repository.WorkerRepository;
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

import com.example.taskassignment.exception.ConflictException;
import com.example.taskassignment.repository.AssignmentRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkerServiceTest {

    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private WorkerService workerService;

    private Worker sampleWorker;

    @BeforeEach
    void setUp() {
        sampleWorker = new Worker("Alice Smith", 8, Set.of("java", "sql"));
        sampleWorker.setId(1L);
    }

    @Test
    @DisplayName("createWorker should trim name, normalize skills, and save")
    void createWorker_shouldNormalizeAndSave() {
        WorkerRequestDto request = new WorkerRequestDto("  Bob Jones  ", 6, Set.of(" Java ", "DOCKER "));

        when(workerRepository.save(any(Worker.class))).thenAnswer(invocation -> {
            Worker toSave = invocation.getArgument(0);
            toSave.setId(20L);
            return toSave;
        });

        WorkerResponseDto response = workerService.createWorker(request);

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.name()).isEqualTo("Bob Jones");
        assertThat(response.hoursPerDay()).isEqualTo(6);
        assertThat(response.skills()).containsExactlyInAnyOrder("java", "docker");

        ArgumentCaptor<Worker> captor = ArgumentCaptor.forClass(Worker.class);
        verify(workerRepository).save(captor.capture());
        Worker captured = captor.getValue();
        assertThat(captured.getName()).isEqualTo("Bob Jones");
        assertThat(captured.getHoursPerDay()).isEqualTo(6);
        assertThat(captured.getSkills()).containsExactlyInAnyOrder("java", "docker");
    }

    @Test
    @DisplayName("getWorkerById should return WorkerResponseDto when worker exists")
    void getWorkerById_shouldReturnWorker() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(sampleWorker));

        WorkerResponseDto response = workerService.getWorkerById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Alice Smith");
        assertThat(response.hoursPerDay()).isEqualTo(8);
        assertThat(response.skills()).containsExactlyInAnyOrder("java", "sql");
    }

    @Test
    @DisplayName("getWorkerById should throw ResourceNotFoundException when worker does not exist")
    void getWorkerById_notFound_shouldThrowException() {
        when(workerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workerService.getWorkerById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Worker not found with id: 99");
    }

    @Test
    @DisplayName("getAllWorkers should return all workers mapped to DTOs")
    void getAllWorkers_shouldReturnList() {
        when(workerRepository.findAll()).thenReturn(List.of(sampleWorker));

        List<WorkerResponseDto> all = workerService.getAllWorkers();

        assertThat(all).hasSize(1);
        assertThat(all.getFirst().name()).isEqualTo("Alice Smith");
    }

    @Test
    @DisplayName("updateWorker should update fields and normalize skills")
    void updateWorker_shouldUpdateFields() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(sampleWorker));
        when(workerRepository.save(any(Worker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkerRequestDto updateRequest = new WorkerRequestDto("Alice Brown", 4, Set.of(" AWS "));

        WorkerResponseDto response = workerService.updateWorker(1L, updateRequest);

        assertThat(response.name()).isEqualTo("Alice Brown");
        assertThat(response.hoursPerDay()).isEqualTo(4);
        assertThat(response.skills()).containsExactly("aws");
    }

    @Test
    @DisplayName("updateWorker should throw ResourceNotFoundException when worker does not exist")
    void updateWorker_notFound_shouldThrowException() {
        when(workerRepository.findById(99L)).thenReturn(Optional.empty());

        WorkerRequestDto updateRequest = new WorkerRequestDto("Alice Brown", 4, Set.of());

        assertThatThrownBy(() -> workerService.updateWorker(99L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Worker not found with id: 99");
    }

    @Test
    @DisplayName("deleteWorker should delete worker when it exists and has no assignments")
    void deleteWorker_shouldDelete() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(sampleWorker));
        when(assignmentRepository.existsByWorkerId(1L)).thenReturn(false);

        workerService.deleteWorker(1L);

        verify(workerRepository).delete(sampleWorker);
    }

    @Test
    @DisplayName("deleteWorker should throw ResourceNotFoundException when worker does not exist")
    void deleteWorker_notFound_shouldThrowException() {
        when(workerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workerService.deleteWorker(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Worker not found with id: 99");
    }

    @Test
    @DisplayName("SPEC Section 6: deleteWorker should throw ConflictException if worker has active assignments")
    void deleteWorker_withAssignments_throwsConflict() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(sampleWorker));
        when(assignmentRepository.existsByWorkerId(1L)).thenReturn(true);

        assertThatThrownBy(() -> workerService.deleteWorker(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot delete worker with active assignments");
    }

    @Test
    @DisplayName("SPEC Section 6: updateWorker should throw ConflictException if worker has assignments and hoursPerDay changes")
    void updateWorker_withAssignmentsHoursChanged_throwsConflict() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(sampleWorker));
        when(assignmentRepository.existsByWorkerId(1L)).thenReturn(true);

        WorkerRequestDto updateDto = new WorkerRequestDto("Alice Smith", 6, Set.of("java", "sql"));

        assertThatThrownBy(() -> workerService.updateWorker(1L, updateDto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot modify hoursPerDay or skills of a worker with active assignments");
    }

    @Test
    @DisplayName("SPEC Section 6: updateWorker should throw ConflictException if worker has assignments and skills change")
    void updateWorker_withAssignmentsSkillsChanged_throwsConflict() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(sampleWorker));
        when(assignmentRepository.existsByWorkerId(1L)).thenReturn(true);

        WorkerRequestDto updateDto = new WorkerRequestDto("Alice Smith", 8, Set.of("java", "sql", "kotlin"));

        assertThatThrownBy(() -> workerService.updateWorker(1L, updateDto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot modify hoursPerDay or skills of a worker with active assignments");
    }

    @Test
    @DisplayName("SPEC Section 6: updateWorker should succeed if worker has assignments but only name changes")
    void updateWorker_withAssignmentsOnlyNameChanged_succeeds() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(sampleWorker));
        when(assignmentRepository.existsByWorkerId(1L)).thenReturn(true);
        when(workerRepository.save(any(Worker.class))).thenAnswer(i -> i.getArgument(0));

        WorkerRequestDto updateDto = new WorkerRequestDto("Alice Johnson", 8, Set.of("java", "sql"));

        WorkerResponseDto response = workerService.updateWorker(1L, updateDto);

        assertThat(response.name()).isEqualTo("Alice Johnson");
        assertThat(response.hoursPerDay()).isEqualTo(8);
        assertThat(response.skills()).containsExactlyInAnyOrder("java", "sql");
    }
}

