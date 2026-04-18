package com.taskoryx.backend.dto.request.role;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateProjectRoleRequest {

    @Size(max = 50, message = "Tên vai trò tối đa 50 ký tự")
    private String name;

    @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
    private String description;

    private Boolean isDefault;

    private List<String> permissions;
}
