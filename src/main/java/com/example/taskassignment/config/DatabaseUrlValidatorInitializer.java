package com.example.taskassignment.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Arrays;

/**
 * Validates that DB_URL is properly provided and formatted with the 'jdbc:' prefix
 * when the 'prod' profile is activated, failing fast with a clear diagnostic message.
 */
public class DatabaseUrlValidatorInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment env = applicationContext.getEnvironment();
        boolean isProd = Arrays.asList(env.getActiveProfiles()).contains("prod");
        if (!isProd) {
            return;
        }

        String dbUrl = env.getProperty("DB_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            throw new IllegalStateException(
                "Production configuration error: DB_URL environment variable is missing. " +
                "Production profile requires a PostgreSQL JDBC URL, e.g. jdbc:postgresql://<host>:<port>/<database>?sslmode=require"
            );
        }

        if (!dbUrl.startsWith("jdbc:")) {
            throw new IllegalStateException(
                "Production configuration error: DB_URL must start with 'jdbc:'. " +
                "Received: '" + dbUrl + "'. " +
                "If using a cloud provider (such as Neon or Render) providing a connection string starting with 'postgresql://' or 'postgres://', " +
                "please prefix it with 'jdbc:' (e.g. jdbc:postgresql://<host>/<database>?sslmode=require)."
            );
        }
    }
}

