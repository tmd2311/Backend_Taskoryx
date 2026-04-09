package com.taskoryx.backend.dto.request.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateMemberRoleRequest {

    @NotBlank(message = "Vai trò không được để trống")
    @Size(max = 50, message = "Tên vai trò tối đa 50 ký tự")
    private String role;
}
