package com.example.taskassignment.service;

import com.example.taskassignment.domain.Worker;
import com.example.taskassignment.dto.WorkerRequestDto;
import com.example.taskassignment.dto.WorkerResponseDto;
import com.example.taskassignment.exception.ConflictException;
import com.example.taskassignment.exception.ResourceNotFoundException;
import com.example.taskassignment.repository.AssignmentRepository;
import com.example.taskassignment.repository.WorkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class WorkerService {

    private final WorkerRepository workerRepository;
    private final AssignmentRepository assignmentRepository;

    public WorkerService(WorkerRepository workerRepository, AssignmentRepository assignmentRepository) {
        this.workerRepository = workerRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkerResponseDto> getAllWorkers() {
        return workerRepository.findAll().stream()
                .map(WorkerResponseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkerResponseDto getWorkerById(Long id) {
        Worker worker = workerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with id: " + id));
        return WorkerResponseDto.fromEntity(worker);
    }

    public WorkerResponseDto createWorker(WorkerRequestDto dto) {
        Set<String> normalizedSkills = SkillUtils.normalizeSkills(dto.skills());

        Worker worker = new Worker(
                dto.name().trim(),
                dto.hoursPerDay(),
                normalizedSkills
        );

        Worker saved = workerRepository.save(worker);
        return WorkerResponseDto.fromEntity(saved);
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
        return WorkerResponseDto.fromEntity(updated);
    }

    public void deleteWorker(Long id) {
        Worker worker = workerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with id: " + id));

        // SPEC Section 6: Deleting a worker with active assignments throws 409 Conflict.
        if (assignmentRepository.existsByWorkerId(id)) {
            throw new ConflictException("Cannot delete worker with active assignments");
        }

        workerRepository.delete(worker);
    }
}
