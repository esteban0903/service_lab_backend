package com.example.taskassignment.service;

import java.time.LocalDate;
import java.util.List;

public record Placement(
        LocalDate effectiveStartDate,
        LocalDate endDate,
        List<DailyAllocation> allocations
) {
}

