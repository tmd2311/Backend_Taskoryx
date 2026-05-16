package com.taskoryx.backend.dto.request.admin;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateRoleRequest {

    @Size(max = 100, message = "Tên hiển thị không được vượt quá 100 ký tự")
    private String displayName;

    @Size(max = 255)
    private String description;
}
