package com.taskoryx.backend.dto.response.version;

import com.taskoryx.backend.entity.Version;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class VersionResponse {

    private UUID id;
    private UUID projectId;
    private String projectKey;
    private String name;
    private String description;
    private Version.VersionStatus status;
    private LocalDate dueDate;
    private LocalDate releaseDate;
    private int totalTasks;
    private int completedTasks;
    private int completionPercent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static VersionResponse from(Version v) {
        return VersionResponse.builder()
                .id(v.getId())
                .projectId(v.getProject().getId())
                .projectKey(v.getProject().getKey())
                .name(v.getName())
                .description(v.getDescription())
                .status(v.getStatus())
                .dueDate(v.getDueDate())
                .releaseDate(v.getReleaseDate())
                .totalTasks(v.getTotalTasks())
                .completedTasks(v.getCompletedTasks())
                .completionPercent(v.getCompletionPercent())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }
}
