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
                // Images
                "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml",
                // Documents
                "application/pdf", "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/rtf", "text/rtf",
                // Spreadsheets
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "text/csv",
                // Presentations
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                // Archives
                "application/zip", "application/x-zip-compressed",
                "application/x-rar-compressed", "application/x-7z-compressed",
                "application/gzip", "application/x-tar",
                // Plain text & markup
                "text/plain", "text/markdown", "text/x-markdown",
                // Code files
                "text/x-python", "text/x-java-source", "text/x-csrc", "text/x-c++src",
                "text/x-csharp", "text/x-go", "text/x-rust", "text/x-kotlin",
                "text/x-swift", "text/x-ruby", "text/x-php",
                "text/html", "text/css", "text/javascript",
                "application/javascript", "application/typescript",
                "application/json", "application/xml", "text/xml",
                "application/x-sh", "text/x-shellscript",
                "application/x-yaml", "text/yaml"
        );
    }
}
