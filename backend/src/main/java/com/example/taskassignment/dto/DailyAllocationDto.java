package com.example.taskassignment.dto;

import java.time.LocalDate;

public record DailyAllocationDto(
        LocalDate date,
        int hours
) {
}

