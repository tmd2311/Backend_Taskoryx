package com.taskoryx.backend.dto.request.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AssignRoleRequest {

    @NotNull(message = "roleId không được để trống")
    private UUID roleId;
}
