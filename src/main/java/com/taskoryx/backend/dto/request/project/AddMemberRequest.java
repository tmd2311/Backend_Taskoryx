package com.taskoryx.backend.dto.request.project;

import com.taskoryx.backend.entity.ProjectMember;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddMemberRequest {

    @NotBlank(message = "Email thành viên không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotNull(message = "Vai trò không được để trống")
    private ProjectMember.ProjectRole role;
}
