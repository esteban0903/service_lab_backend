package com.example.taskassignment.controller;

import com.example.taskassignment.dto.ChangePasswordRequestDto;
import com.example.taskassignment.dto.LoginRequestDto;
import com.example.taskassignment.dto.LoginResponseDto;
import com.example.taskassignment.dto.UserProfileDto;
import com.example.taskassignment.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponseDto login(@Valid @RequestBody LoginRequestDto request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserProfileDto getProfile(@AuthenticationPrincipal Jwt jwt) {
        return authService.getProfile(jwt.getSubject());
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequestDto request
    ) {
        authService.changePassword(jwt.getSubject(), request);
    }
}

