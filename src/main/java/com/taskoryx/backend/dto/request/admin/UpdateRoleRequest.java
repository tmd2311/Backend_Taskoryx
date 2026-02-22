package com.taskoryx.backend.dto.request.admin;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateRoleRequest {

    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String description;
}
