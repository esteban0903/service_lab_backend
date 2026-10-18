package com.example.taskassignment.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("JwtSecretValidatorInitializer Tests")
class JwtSecretValidatorInitializerTest {

    private final JwtSecretValidatorInitializer initializer = new JwtSecretValidatorInitializer();

    @Test
    @DisplayName("Dev profile ignores JWT_SECRET check")
    void initialize_devProfile_ignoresMissingJwtSecret() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");

        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        when(context.getEnvironment()).thenReturn(env);

        assertThatCode(() -> initializer.initialize(context)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Prod profile throws IllegalStateException if JWT_SECRET is missing")
    void initialize_prodProfile_missingJwtSecret_throwsException() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        when(context.getEnvironment()).thenReturn(env);

        assertThatThrownBy(() -> initializer.initialize(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET environment variable is missing")
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    @DisplayName("Prod profile throws IllegalStateException if JWT_SECRET is blank")
    void initialize_prodProfile_blankJwtSecret_throwsException() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("JWT_SECRET", "   ");

        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        when(context.getEnvironment()).thenReturn(env);

        assertThatThrownBy(() -> initializer.initialize(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET environment variable is missing");
    }

    @Test
    @DisplayName("Prod profile throws IllegalStateException if JWT_SECRET is shorter than 32 bytes")
    void initialize_prodProfile_shortJwtSecret_throwsException() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("JWT_SECRET", "too-short-secret-key-16b"); // 24 bytes

        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        when(context.getEnvironment()).thenReturn(env);

        assertThatThrownBy(() -> initializer.initialize(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET must be at least 32 bytes");
    }

    @Test
    @DisplayName("Prod profile succeeds when JWT_SECRET is at least 32 bytes")
    void initialize_prodProfile_validJwtSecret_succeeds() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("JWT_SECRET", "a-sufficiently-long-secret-key-that-has-at-least-32-bytes");

        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        when(context.getEnvironment()).thenReturn(env);

        assertThatCode(() -> initializer.initialize(context)).doesNotThrowAnyException();
    }
}

