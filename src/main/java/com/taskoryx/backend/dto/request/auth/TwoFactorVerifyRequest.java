package com.taskoryx.backend.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TwoFactorVerifyRequest {
    @NotBlank(message = "Mã xác thực không được để trống")
    @Size(min = 6, max = 8)
    private String code;
}
