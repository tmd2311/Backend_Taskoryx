package com.taskoryx.backend.dto.response.project;

import com.taskoryx.backend.entity.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private UUID id;
    private String name;
    private String description;
    private String key;
    private String icon;
    private String color;
    private Boolean isPublic;
    private Boolean isArchived;
    private String ownerName;
    private UUID ownerId;
    private int memberCount;
    private int taskCount;
    private String currentUserRole;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProjectResponse from(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .key(project.getKey())
                .icon(project.getIcon())
                .color(project.getColor())
                .isPublic(project.getIsPublic())
                .isArchived(project.getIsArchived())
                .ownerName(project.getOwner().getFullName())
                .ownerId(project.getOwner().getId())
                .memberCount(project.getMembers().size())
                .taskCount(project.getTasks().size())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
