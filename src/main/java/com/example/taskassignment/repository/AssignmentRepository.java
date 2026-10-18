package com.example.taskassignment.repository;

import com.example.taskassignment.domain.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    Optional<Assignment> findByTaskId(Long taskId);

    boolean existsByTaskId(Long taskId);

    boolean existsByWorkerId(Long workerId);

    java.util.List<Assignment> findByWorkerId(Long workerId);

    void deleteByTaskId(Long taskId);
}

