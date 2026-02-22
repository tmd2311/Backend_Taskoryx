package com.taskoryx.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Taskoryx Backend Application
 * Main entry point for Spring Boot application
 *
 * @author Taskoryx Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
public class TaskoryxApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskoryxApplication.class, args);
    }

}
