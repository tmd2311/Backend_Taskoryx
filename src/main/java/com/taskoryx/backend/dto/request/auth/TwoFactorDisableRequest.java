package com.taskoryx.backend.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TwoFactorDisableRequest {
    @NotBlank
    private String password; // Require password to disable 2FA
}
