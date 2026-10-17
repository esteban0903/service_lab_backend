package com.example.taskassignment;

import com.example.taskassignment.config.DatabaseUrlValidatorInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TaskAssignmentApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(TaskAssignmentApplication.class);
        app.addInitializers(new DatabaseUrlValidatorInitializer());
        app.run(args);
    }
}

