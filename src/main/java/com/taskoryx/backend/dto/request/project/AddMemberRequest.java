package com.taskoryx.backend.dto.request.project;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddMemberRequest {

    @NotBlank(message = "Email thành viên không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;
}
