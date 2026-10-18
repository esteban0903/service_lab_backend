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

    @org.springframework.data.jpa.repository.Query(
        "SELECT DISTINCT a FROM Assignment a " +
        "JOIN FETCH a.task t " +
        "JOIN FETCH a.worker w " +
        "LEFT JOIN FETCH a.assignmentDays ad"
    )
    java.util.List<Assignment> findAllWithDetails();

    @org.springframework.data.jpa.repository.Query(
        "SELECT DISTINCT a FROM Assignment a " +
        "JOIN FETCH a.task t " +
        "JOIN FETCH a.worker w " +
        "LEFT JOIN FETCH a.assignmentDays ad " +
        "WHERE a.worker.id = :workerId"
    )
    java.util.List<Assignment> findByWorkerIdWithDetails(@org.springframework.data.repository.query.Param("workerId") Long workerId);

    java.util.List<Assignment> findByWorkerId(Long workerId);

    void deleteByTaskId(Long taskId);
}

