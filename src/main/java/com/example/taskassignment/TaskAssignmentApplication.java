package com.example.taskassignment;

import com.example.taskassignment.config.DatabaseUrlValidatorInitializer;
import com.example.taskassignment.config.JwtSecretValidatorInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TaskAssignmentApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(TaskAssignmentApplication.class);
        app.addInitializers(new DatabaseUrlValidatorInitializer(), new JwtSecretValidatorInitializer());
        app.run(args);
    }
}

