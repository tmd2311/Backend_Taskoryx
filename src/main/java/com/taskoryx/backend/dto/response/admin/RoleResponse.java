package com.taskoryx.backend.dto.response.admin;

import com.taskoryx.backend.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
public class RoleResponse {
    private UUID id;
    private String name;
    private String description;
    private Boolean isSystemRole;
    private Set<PermissionResponse> permissions;
    private LocalDateTime createdAt;

    public static RoleResponse from(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .isSystemRole(role.getIsSystemRole())
                .permissions(role.getPermissions().stream()
                        .map(PermissionResponse::from)
                        .collect(Collectors.toSet()))
                .createdAt(role.getCreatedAt())
                .build();
    }
}
