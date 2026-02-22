package com.taskoryx.backend.dto.request.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class AssignPermissionsRequest {

    @NotNull(message = "permissionIds không được để trống")
    private Set<UUID> permissionIds;
}
