package com.example.taskassignment.repository;

import com.example.taskassignment.domain.AssignmentDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AssignmentDayRepository extends JpaRepository<AssignmentDay, Long> {

    @Query("SELECT ad.date AS date, SUM(ad.hours) AS hours " +
            "FROM AssignmentDay ad " +
            "WHERE ad.assignment.worker.id = :workerId AND ad.date >= :startDate " +
            "GROUP BY ad.date")
    List<WorkerDailyLoadProjection> findWorkerDailyLoadFromDate(@Param("workerId") Long workerId, @Param("startDate") LocalDate startDate);

    @Query("SELECT ad.date AS date, SUM(ad.hours) AS hours " +
            "FROM AssignmentDay ad " +
            "WHERE ad.assignment.worker.id = :workerId " +
            "GROUP BY ad.date " +
            "ORDER BY ad.date ASC")
    List<WorkerDailyLoadProjection> findWorkerSchedule(@Param("workerId") Long workerId);
}

