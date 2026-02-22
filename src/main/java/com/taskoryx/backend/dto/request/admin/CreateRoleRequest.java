package com.taskoryx.backend.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class CreateRoleRequest {

    @NotBlank(message = "Tên role không được để trống")
    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String description;

    private Set<UUID> permissionIds;
}
