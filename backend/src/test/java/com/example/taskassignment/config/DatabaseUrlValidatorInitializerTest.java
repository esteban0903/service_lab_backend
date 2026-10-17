package com.example.taskassignment.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("DatabaseUrlValidatorInitializer Tests")
class DatabaseUrlValidatorInitializerTest {

    private final DatabaseUrlValidatorInitializer initializer = new DatabaseUrlValidatorInitializer();

    @Test
    @DisplayName("Dev profile ignores DB_URL check")
    void initialize_devProfile_ignoresMissingDbUrl() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");

        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        when(context.getEnvironment()).thenReturn(env);

        assertThatCode(() -> initializer.initialize(context)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Prod profile throws IllegalStateException if DB_URL is missing")
    void initialize_prodProfile_missingDbUrl_throwsException() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        when(context.getEnvironment()).thenReturn(env);

        assertThatThrownBy(() -> initializer.initialize(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_URL environment variable is missing")
                .hasMessageContaining("jdbc:postgresql://");
    }

    @Test
    @DisplayName("Prod profile throws IllegalStateException if DB_URL is blank")
    void initialize_prodProfile_blankDbUrl_throwsException() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("DB_URL", "   ");

        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        when(context.getEnvironment()).thenReturn(env);

        assertThatThrownBy(() -> initializer.initialize(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_URL environment variable is missing");
    }

    @Test
    @DisplayName("Prod profile throws IllegalStateException if DB_URL does not start with jdbc:")
    void initialize_prodProfile_nonJdbcDbUrl_throwsException() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("DB_URL", "postgresql://ep-cool-proj.neon.tech/neondb?sslmode=require");

        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        when(context.getEnvironment()).thenReturn(env);

        assertThatThrownBy(() -> initializer.initialize(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_URL must start with 'jdbc:'")
                .hasMessageContaining("prefix it with 'jdbc:'");
    }

    @Test
    @DisplayName("Prod profile succeeds when DB_URL starts with jdbc:")
    void initialize_prodProfile_validJdbcUrl_succeeds() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("DB_URL", "jdbc:postgresql://ep-cool-proj.neon.tech/neondb?sslmode=require");

        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        when(context.getEnvironment()).thenReturn(env);

        assertThatCode(() -> initializer.initialize(context)).doesNotThrowAnyException();
    }
}

