package com.taskoryx.backend.dto.response.admin;

import com.taskoryx.backend.entity.Permission;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PermissionResponse {
    private UUID id;
    private String name;
    private String description;
    private String resource;

    public static PermissionResponse from(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .resource(permission.getResource())
                .build();
    }
}
