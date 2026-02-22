package com.taskoryx.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "application")
@Getter
@Setter
public class AppProperties {

    private String name = "Taskoryx";
    private String version = "1.0.0";
    private String frontendUrl = "http://localhost:3000";

    private Cors cors = new Cors();
    private Storage storage = new Storage();

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:3000");
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        private List<String> allowedHeaders = List.of("*");
        private boolean allowCredentials = true;
        private long maxAge = 3600;
    }

    @Getter
    @Setter
    public static class Storage {
        private String uploadDir = "uploads";
        private long maxFileSize = 10 * 1024 * 1024; // 10MB
        private List<String> allowedTypes = List.of(
                "image/jpeg", "image/png", "image/gif", "image/webp",
                "application/pdf", "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "text/plain", "application/zip"
        );
    }
}
