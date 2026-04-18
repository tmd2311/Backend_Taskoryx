package com.taskoryx.backend.dto.request.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateProjectRoleRequest {

    @NotBlank(message = "Tên vai trò không được để trống")
    @Size(max = 50, message = "Tên vai trò tối đa 50 ký tự")
    private String name;

    @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
    private String description;

    private boolean isDefault = false;

    private List<String> permissions;
}
