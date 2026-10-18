package com.example.taskassignment.repository;

import com.example.taskassignment.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT t FROM Task t WHERE t.status = com.example.taskassignment.domain.TaskStatus.NOT_DONE AND t.id NOT IN (SELECT a.task.id FROM Assignment a)")
    List<Task> findUnassignedNotDoneTasks();

    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN FETCH t.skills")
    List<Task> findAllWithSkills();
}

