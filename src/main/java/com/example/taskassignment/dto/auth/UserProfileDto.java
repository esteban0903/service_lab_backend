package com.example.taskassignment.dto.auth;

import com.example.taskassignment.domain.Role;

public record UserProfileDto(
        Long id,
        String email,
        Role role,
        Long workerId
) {
}

