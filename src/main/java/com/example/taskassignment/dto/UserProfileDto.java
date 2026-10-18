package com.example.taskassignment.dto;

import com.example.taskassignment.model.Role;

public record UserProfileDto(
        Long id,
        String email,
        Role role,
        Long workerId
) {
}

