package com.example.taskassignment.repository;

import java.time.LocalDate;

public interface WorkerDailyLoadProjection {
    LocalDate getDate();
    Integer getHours();
}

