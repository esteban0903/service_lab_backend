package com.example.taskassignment.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Validates that JWT_SECRET is properly provided and at least 32 bytes (256 bits)
 * when the 'prod' profile is activated, failing fast with a clear diagnostic message.
 */
public class JwtSecretValidatorInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment env = applicationContext.getEnvironment();
        boolean isProd = Arrays.asList(env.getActiveProfiles()).contains("prod");
        if (!isProd) {
            return;
        }

        String jwtSecret = env.getProperty("JWT_SECRET");
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                "Production configuration error: JWT_SECRET environment variable is missing. " +
                "Production profile requires a secure secret of at least 32 bytes (256 bits) for HMAC-SHA256 signing."
            );
        }

        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                "Production configuration error: JWT_SECRET must be at least 32 bytes (256 bits) long for HMAC-SHA256. " +
                "Current length: " + secretBytes.length + " bytes."
            );
        }
    }
}

