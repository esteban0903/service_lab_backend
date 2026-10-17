package com.example.taskassignment.service;

import com.example.taskassignment.domain.Worker;
import com.example.taskassignment.dto.WorkerRequestDto;
import com.example.taskassignment.dto.WorkerResponseDto;
import com.example.taskassignment.exception.ResourceNotFoundException;
import com.example.taskassignment.repository.WorkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class WorkerService {

    private final WorkerRepository workerRepository;

    public WorkerService(WorkerRepository workerRepository) {
        this.workerRepository = workerRepository;
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

        // TODO Phase 2: Check if worker has assignments. Editing hoursPerDay or skills of a worker with assignments must throw ConflictException (409).

        worker.setName(dto.name().trim());
        worker.setHoursPerDay(dto.hoursPerDay());
        worker.setSkills(SkillUtils.normalizeSkills(dto.skills()));

        Worker updated = workerRepository.save(worker);
        return WorkerResponseDto.fromEntity(updated);
    }

    public void deleteWorker(Long id) {
        Worker worker = workerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with id: " + id));

        // TODO Phase 2: Check if worker has assignments. Deleting a worker with assignments must throw ConflictException (409).

        workerRepository.delete(worker);
    }
}

