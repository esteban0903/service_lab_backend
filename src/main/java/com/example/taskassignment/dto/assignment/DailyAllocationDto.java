package com.example.taskassignment.dto.assignment;

import java.time.LocalDate;

public record DailyAllocationDto(
        LocalDate date,
        int hours
) {
}

