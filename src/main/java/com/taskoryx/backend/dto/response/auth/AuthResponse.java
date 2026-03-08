package com.taskoryx.backend.dto.response.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả đăng nhập")
public class AuthResponse {

    @Schema(description = "Access token (hết hạn sau 24h)", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI...")
    private String accessToken;

    @Schema(description = "Refresh token (hết hạn sau 7 ngày)", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI...")
    private String refreshToken;

    @Builder.Default
    @Schema(description = "Loại token", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "Thời gian hết hạn access token (ms)", example = "86400000")
    private long expiresIn;

    @Schema(description = "ID người dùng", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID userId;

    @Schema(description = "Tên đăng nhập", example = "nguyenvana")
    private String username;

    @Schema(description = "Email", example = "admin@taskoryx.com")
    private String email;

    @Schema(description = "Họ và tên", example = "Nguyễn Văn A")
    private String fullName;

    @Schema(description = "URL ảnh đại diện", example = "https://example.com/avatar.jpg", nullable = true)
    private String avatarUrl;

    @Schema(description = "Người dùng phải đổi mật khẩu sau lần đăng nhập đầu tiên", example = "true")
    private boolean mustChangePassword;
}
