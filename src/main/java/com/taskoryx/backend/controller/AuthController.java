package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.request.auth.LoginRequest;
import com.taskoryx.backend.dto.request.auth.RefreshTokenRequest;
import com.taskoryx.backend.dto.response.ApiResponse;
import com.taskoryx.backend.dto.response.auth.AuthResponse;
import com.taskoryx.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller cho Authentication
 *
 * POST /api/auth/login     - Đăng nhập
 * POST /api/auth/refresh   - Làm mới access token
 * POST /api/auth/logout    - Đăng xuất
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Đăng nhập và quản lý token")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Làm mới access token bằng refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token đã được làm mới", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất (client xóa token)")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // JWT là stateless - client tự xóa token
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công"));
    }
}
