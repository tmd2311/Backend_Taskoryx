package com.taskoryx.backend.dto.request.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 100, message = "Họ tên không được quá 100 ký tự")
    private String fullName;

    @Size(max = 20, message = "Số điện thoại không được quá 20 ký tự")
    private String phone;

    private String avatarUrl;
    private String timezone;
    private String language;
}
