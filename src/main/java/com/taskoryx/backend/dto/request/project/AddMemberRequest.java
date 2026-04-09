package com.taskoryx.backend.dto.request.project;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddMemberRequest {

    @NotBlank(message = "Email thành viên không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    // Nullable: nếu user có system role PM thì tự động gán "PM", không cần truyền
    @Size(max = 50, message = "Tên vai trò tối đa 50 ký tự")
    private String role;
}
