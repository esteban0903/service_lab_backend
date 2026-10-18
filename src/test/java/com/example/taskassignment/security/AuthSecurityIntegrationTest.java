package com.example.taskassignment.security;

import com.example.taskassignment.dto.ChangePasswordRequestDto;
import com.example.taskassignment.dto.LoginRequestDto;
import com.example.taskassignment.dto.LoginResponseDto;
import com.example.taskassignment.domain.Role;
import com.example.taskassignment.domain.User;
import com.example.taskassignment.repository.UserRepository;
import com.example.taskassignment.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@DisplayName("Authentication & JWT Security Integration Tests")
class AuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private AuthService authService;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByEmail("admin@demo.com")) {
            userRepository.save(new User("admin@demo.com", passwordEncoder.encode("Admin123!"), Role.ADMIN, null));
        }
    }

    @Test
    @DisplayName("Public endpoints GET /api/health and /api/config are reachable without a token")
    void publicEndpoints_reachableWithoutToken() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));

        mockMvc.perform(get("/api/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seedEnabled", is(true)));
    }

    @Test
    @DisplayName("Protected endpoint GET /api/tasks without token returns 401 Unauthorized")
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("POST /api/auth/login with valid admin credentials returns token and user info")
    void login_validCredentials_returnsToken() throws Exception {
        LoginRequestDto request = new LoginRequestDto("admin@demo.com", "Admin123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role", is("ADMIN")))
                .andExpect(jsonPath("$.email", is("admin@demo.com")));
    }

    @Test
    @DisplayName("POST /api/auth/login with wrong password returns generic 401")
    void login_wrongPassword_returns401() throws Exception {
        LoginRequestDto request = new LoginRequestDto("admin@demo.com", "WrongPassword!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("POST /api/auth/login with non-existent email returns identical generic 401 (constant-time dummy check)")
    void login_unknownEmail_returns401() throws Exception {
        LoginRequestDto request = new LoginRequestDto("unknown@demo.com", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("POST /api/auth/login with disabled user returns generic 401")
    void login_disabledUser_returns401() throws Exception {
        User disabledUser = new User("disabled@demo.com", passwordEncoder.encode("Secret123!"), Role.WORKER, null);
        disabledUser.setEnabled(false);
        userRepository.save(disabledUser);

        LoginRequestDto request = new LoginRequestDto("disabled@demo.com", "Secret123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("Expired token returns 401 Unauthorized")
    void expiredToken_returns401() throws Exception {
        Instant past = Instant.now().minus(2, ChronoUnit.HOURS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("servicelab")
                .issuedAt(past.minus(1, ChronoUnit.HOURS))
                .expiresAt(past)
                .subject("admin@demo.com")
                .claim("role", "ADMIN")
                .build();
        JwsHeader header = JwsHeader.with(() -> "HS256").build();
        String expiredToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Tampered token returns 401 Unauthorized")
    void tamperedToken_returns401() throws Exception {
        LoginResponseDto login = authService.login(new LoginRequestDto("admin@demo.com", "Admin123!"));
        String tamperedToken = login.token() + "tampered";

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/auth/me returns authenticated user profile")
    void getMe_withValidToken_returnsUserProfile() throws Exception {
        LoginResponseDto login = authService.login(new LoginRequestDto("admin@demo.com", "Admin123!"));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + login.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("admin@demo.com")))
                .andExpect(jsonPath("$.role", is("ADMIN")));
    }

    @Test
    @DisplayName("POST /api/auth/change-password changes user password and enables login with new password")
    void changePassword_validRequest_updatesPassword() throws Exception {
        LoginResponseDto login = authService.login(new LoginRequestDto("admin@demo.com", "Admin123!"));

        ChangePasswordRequestDto request = new ChangePasswordRequestDto("Admin123!", "NewSecret123!");

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + login.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // Verify login succeeds with new password
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDto("admin@demo.com", "NewSecret123!"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/auth/change-password with wrong current password returns 401 Unauthorized")
    void changePassword_wrongCurrentPassword_returns401() throws Exception {
        LoginResponseDto login = authService.login(new LoginRequestDto("admin@demo.com", "Admin123!"));

        ChangePasswordRequestDto request = new ChangePasswordRequestDto("WrongCurrentPass!", "NewValidSecret123!");

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + login.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("POST /api/auth/change-password with new password shorter than 8 characters returns 400 Bad Request")
    void changePassword_tooShort_returns400() throws Exception {
        LoginResponseDto login = authService.login(new LoginRequestDto("admin@demo.com", "Admin123!"));

        ChangePasswordRequestDto request = new ChangePasswordRequestDto("Admin123!", "short");

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + login.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }
}

