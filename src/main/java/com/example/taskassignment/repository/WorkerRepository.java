package com.example.taskassignment.repository;

import com.example.taskassignment.domain.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, Long> {

    @Query("SELECT DISTINCT w FROM Worker w LEFT JOIN FETCH w.skills")
    List<Worker> findAllWithSkills();
}

