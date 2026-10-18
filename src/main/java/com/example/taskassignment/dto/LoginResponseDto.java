package com.example.taskassignment.dto;

import com.example.taskassignment.domain.Role;

import java.time.Instant;

public record LoginResponseDto(
        String token,
        Instant expiresAt,
        Role role,
        String email,
        Long workerId
) {
}

