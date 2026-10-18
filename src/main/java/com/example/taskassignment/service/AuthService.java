package com.example.taskassignment.service;

import com.example.taskassignment.domain.Worker;
import com.example.taskassignment.dto.ChangePasswordRequestDto;
import com.example.taskassignment.dto.LoginRequestDto;
import com.example.taskassignment.dto.LoginResponseDto;
import com.example.taskassignment.dto.UserProfileDto;
import com.example.taskassignment.exception.BadRequestException;
import com.example.taskassignment.exception.ConflictException;
import com.example.taskassignment.exception.ResourceNotFoundException;
import com.example.taskassignment.model.Role;
import com.example.taskassignment.model.User;
import com.example.taskassignment.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final Environment environment;
    private final long expirationMinutes;
    private final String adminEmailConfig;
    private final String adminPasswordConfig;

    // Dummy hash computed once so login attempts for non-existent emails incur standard BCrypt verification time
    private final String dummyHash;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            Clock clock,
            Environment environment,
            @Value("${app.security.jwt.expiration-minutes:480}") long expirationMinutes,
            @Value("${app.security.admin.email:}") String adminEmailConfig,
            @Value("${app.security.admin.password:}") String adminPasswordConfig
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.environment = environment;
        this.expirationMinutes = expirationMinutes;
        this.adminEmailConfig = adminEmailConfig;
        this.adminPasswordConfig = adminPasswordConfig;
        this.dummyHash = passwordEncoder.encode("dummy-constant-time-password-verification");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeAdminUser() {
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        String normalizedEmail = adminEmailConfig != null ? adminEmailConfig.trim().toLowerCase() : "";

        if (normalizedEmail.isBlank() || adminPasswordConfig == null || adminPasswordConfig.isBlank()) {
            if (isProd) {
                log.warn("No ADMIN_EMAIL and ADMIN_PASSWORD configured in prod profile. Initial admin user was NOT created.");
            }
            return;
        }

        if (!userRepository.existsByEmail(normalizedEmail)) {
            User admin = new User(normalizedEmail, passwordEncoder.encode(adminPasswordConfig), Role.ADMIN, null);
            userRepository.save(admin);
            log.info("Initialized initial admin user: {}", normalizedEmail);
        }
    }

    public LoginResponseDto login(LoginRequestDto request) {
        String normalizedEmail = request.email() != null ? request.email().trim().toLowerCase() : "";
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);

        if (userOpt.isEmpty()) {
            // Constant-time execution: perform BCrypt verification against dummy hash
            passwordEncoder.matches(request.password(), dummyHash);
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.isEnabled()) {
            throw new BadCredentialsException("Invalid email or password");
        }

        Instant now = clock.instant();
        Instant expiresAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);
        Long workerId = user.getWorker() != null ? user.getWorker().getId() : null;

        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer("servicelab")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getEmail())
                .claim("role", user.getRole().name());

        if (workerId != null) {
            claimsBuilder.claim("workerId", workerId);
        }

        JwtClaimsSet claims = claimsBuilder.build();
        JwsHeader header = JwsHeader.with(() -> "HS256").build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new LoginResponseDto(token, expiresAt, user.getRole(), user.getEmail(), workerId);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + normalizedEmail));
        Long workerId = user.getWorker() != null ? user.getWorker().getId() : null;
        return new UserProfileDto(user.getId(), user.getEmail(), user.getRole(), workerId);
    }

    public void changePassword(String email, ChangePasswordRequestDto request) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + normalizedEmail));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid current password");
        }

        if (request.newPassword().length() < 8) {
            throw new BadRequestException("New password must be at least 8 characters long");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    public User createWorkerUser(Worker worker, String email, String password) {
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("User with email '" + normalizedEmail + "' already exists");
        }
        if (password == null || password.length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters long");
        }

        User user = new User(normalizedEmail, passwordEncoder.encode(password), Role.WORKER, worker);
        return userRepository.save(user);
    }

    public void setOrUpdateWorkerCredentials(Long workerId, String email, String password, Worker worker) {
        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> existingEmailUser = userRepository.findByEmail(normalizedEmail);
        if (existingEmailUser.isPresent() && (existingEmailUser.get().getWorker() == null || !existingEmailUser.get().getWorker().getId().equals(workerId))) {
            throw new ConflictException("User with email '" + normalizedEmail + "' already exists");
        }

        if (password == null || password.length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters long");
        }

        Optional<User> existingWorkerUser = userRepository.findByWorkerId(workerId);
        if (existingWorkerUser.isPresent()) {
            User user = existingWorkerUser.get();
            user.setEmail(normalizedEmail);
            user.setPasswordHash(passwordEncoder.encode(password));
            userRepository.save(user);
        } else {
            User newUser = new User(normalizedEmail, passwordEncoder.encode(password), Role.WORKER, worker);
            userRepository.save(newUser);
        }
    }
}
