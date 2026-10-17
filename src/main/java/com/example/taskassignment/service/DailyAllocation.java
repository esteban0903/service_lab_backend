package com.example.taskassignment.service;

import java.time.LocalDate;

public record DailyAllocation(
        LocalDate date,
        int hours
) {
}

