package com.example.taskassignment.dto;

import java.time.LocalDate;

public record AutoAssignmentRequestDto(
        LocalDate startDate
) {
}

