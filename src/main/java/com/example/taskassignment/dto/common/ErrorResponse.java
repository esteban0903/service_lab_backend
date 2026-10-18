package com.example.taskassignment.dto.common;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record ErrorResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant timestamp,
        int status,
        String error,
        String message
) {
    public ErrorResponse(int status, String error, String message) {
        this(Instant.now(), status, error, message);
    }
}

