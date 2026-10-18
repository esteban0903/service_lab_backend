package com.example.taskassignment.service;

import com.example.taskassignment.domain.Worker;
import com.example.taskassignment.dto.WorkerRequestDto;
import com.example.taskassignment.dto.WorkerResponseDto;
import com.example.taskassignment.exception.ConflictException;
import com.example.taskassignment.exception.ResourceNotFoundException;
import com.example.taskassignment.model.User;
import com.example.taskassignment.repository.AssignmentRepository;
import com.example.taskassignment.repository.UserRepository;
import com.example.taskassignment.repository.WorkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class WorkerService {

    private final WorkerRepository workerRepository;
    private final AssignmentRepository assignmentRepository;
    private final AuthService authService;
    private final UserRepository userRepository;

    public WorkerService(
            WorkerRepository workerRepository,
            AssignmentRepository assignmentRepository,
            AuthService authService,
            UserRepository userRepository
    ) {
        this.workerRepository = workerRepository;
        this.assignmentRepository = assignmentRepository;
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkerResponseDto> getAllWorkers() {
        List<Worker> workers = workerRepository.findAllWithSkills();
        if (workers.isEmpty()) {
            return List.of();
        }

        List<Long> workerIds = workers.stream().map(Worker::getId).toList();
        Map<Long, String> workerEmailMap = userRepository.findByWorkerIdIn(workerIds).stream()
                .filter(u -> u.getWorker() != null)
                .collect(Collectors.toMap(u -> u.getWorker().getId(), User::getEmail, (existing, replacement) -> existing));

        return workers.stream()
                .map(w -> WorkerResponseDto.fromEntity(w, workerEmailMap.get(w.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkerResponseDto getWorkerById(Long id) {
        Worker worker = workerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with id: " + id));
        String email = userRepository.findByWorkerId(id).map(User::getEmail).orElse(null);
        return WorkerResponseDto.fromEntity(worker, email);
    }

    public WorkerResponseDto createWorker(WorkerRequestDto dto) {
        Set<String> normalizedSkills = SkillUtils.normalizeSkills(dto.skills());

        boolean hasEmail = dto.email() != null && !dto.email().isBlank();
        boolean hasPassword = dto.temporaryPassword() != null && !dto.temporaryPassword().isBlank();

        if (hasEmail != hasPassword) {
            throw new com.example.taskassignment.exception.BadRequestException("Email and temporaryPassword must both be provided or both omitted");
        }

        Worker worker = new Worker(
                dto.name().trim(),
                dto.hoursPerDay(),
                normalizedSkills
        );

        Worker saved = workerRepository.save(worker);

        String linkedEmail = null;
        if (hasEmail) {
            authService.createWorkerUser(saved, dto.email(), dto.temporaryPassword());
            linkedEmail = dto.email().trim().toLowerCase();
        }

        return WorkerResponseDto.fromEntity(saved, linkedEmail);
    }

    public void updateWorkerCredentials(Long id, com.example.taskassignment.dto.WorkerCredentialsRequestDto dto) {
        Worker worker = workerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with id: " + id));
        authService.setOrUpdateWorkerCredentials(id, dto.email(), dto.password(), worker);
    }

    public WorkerResponseDto updateWorker(Long id, WorkerRequestDto dto) {
        Worker worker = workerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with id: " + id));

        Set<String> normalizedSkills = SkillUtils.normalizeSkills(dto.skills());

        // SPEC Section 6: If worker has assignments, editing hoursPerDay or skills throws 409 Conflict.
        if (assignmentRepository.existsByWorkerId(id)) {
            if (worker.getHoursPerDay() != dto.hoursPerDay() || !worker.getSkills().equals(normalizedSkills)) {
                throw new ConflictException("Cannot modify hoursPerDay or skills of a worker with active assignments");
            }
        }

        worker.setName(dto.name().trim());
        worker.setHoursPerDay(dto.hoursPerDay());
        worker.setSkills(normalizedSkills);

        Worker updated = workerRepository.save(worker);
        String linkedEmail = userRepository.findByWorkerId(id).map(User::getEmail).orElse(null);
        return WorkerResponseDto.fromEntity(updated, linkedEmail);
    }

    public void deleteWorker(Long id) {
        Worker worker = workerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with id: " + id));

        // SPEC Section 6: Deleting a worker with active assignments throws 409 Conflict.
        if (assignmentRepository.existsByWorkerId(id)) {
            throw new ConflictException("Cannot delete worker with active assignments");
        }

        authService.deleteWorkerUser(id);
        workerRepository.delete(worker);
    }
}
