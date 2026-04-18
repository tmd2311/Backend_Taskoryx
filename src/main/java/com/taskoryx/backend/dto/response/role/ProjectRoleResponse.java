package com.taskoryx.backend.dto.response.role;

import com.taskoryx.backend.entity.ProjectRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRoleResponse {

    private UUID id;
    private UUID projectId;
    private String name;
    private String description;
    private boolean isDefault;
    private List<String> permissions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProjectRoleResponse from(ProjectRole role) {
        return ProjectRoleResponse.builder()
                .id(role.getId())
                .projectId(role.getProject().getId())
                .name(role.getName())
                .description(role.getDescription())
                .isDefault(role.isDefault())
                .permissions(role.getPermissionList())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }
}
