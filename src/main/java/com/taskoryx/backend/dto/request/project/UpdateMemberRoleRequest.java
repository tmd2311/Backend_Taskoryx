package com.taskoryx.backend.dto.request.project;

import com.taskoryx.backend.entity.ProjectMember;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMemberRoleRequest {

    @NotNull(message = "Vai trò không được để trống")
    private ProjectMember.ProjectRole role;
}
