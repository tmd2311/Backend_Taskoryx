package com.taskoryx.backend.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorSetupResponse {
    private String secret;       // TOTP secret key (base32)
    private String qrCodeUrl;    // URL for QR code image (otpauth:// URI)
    private String manualCode;   // Manual entry code (same as secret, formatted)
}
