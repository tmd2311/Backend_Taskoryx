package com.taskoryx.backend.dto.request.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangeMemberRoleRequest {

    @NotBlank(message = "Vai trò không được để trống")
    @Size(max = 50, message = "Tên vai trò tối đa 50 ký tự")
    private String role;
}
