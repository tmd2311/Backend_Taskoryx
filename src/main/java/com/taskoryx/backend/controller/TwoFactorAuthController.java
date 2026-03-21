package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.request.auth.TwoFactorDisableRequest;
import com.taskoryx.backend.dto.request.auth.TwoFactorVerifyRequest;
import com.taskoryx.backend.dto.response.ApiResponse;
import com.taskoryx.backend.dto.response.auth.TwoFactorSetupResponse;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.TwoFactorAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/2fa")
@RequiredArgsConstructor
@Tag(name = "Two-Factor Auth", description = "Xác thực hai bước (TOTP / Google Authenticator)")
public class TwoFactorAuthController {

    private final TwoFactorAuthService twoFactorAuthService;

    @PostMapping("/setup")
    @Operation(summary = "Bắt đầu thiết lập 2FA - trả về QR code và secret")
    public ResponseEntity<ApiResponse<TwoFactorSetupResponse>> setup(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Quét mã QR bằng Google Authenticator",
                twoFactorAuthService.setup2FA(principal)));
    }

    @PostMapping("/enable")
    @Operation(summary = "Kích hoạt 2FA sau khi quét QR - xác thực bằng mã TOTP")
    public ResponseEntity<ApiResponse<Void>> enable(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TwoFactorVerifyRequest request) {
        twoFactorAuthService.enable2FA(request, principal);
        return ResponseEntity.ok(ApiResponse.success("Xác thực 2 bước đã được kích hoạt thành công"));
    }

    @PostMapping("/disable")
    @Operation(summary = "Tắt 2FA - cần xác nhận bằng mật khẩu")
    public ResponseEntity<ApiResponse<Void>> disable(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TwoFactorDisableRequest request) {
        twoFactorAuthService.disable2FA(request, principal);
        return ResponseEntity.ok(ApiResponse.success("Xác thực 2 bước đã được tắt"));
    }

    @GetMapping("/status")
    @Operation(summary = "Kiểm tra trạng thái 2FA của tài khoản")
    public ResponseEntity<ApiResponse<Boolean>> status(
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean enabled = twoFactorAuthService.is2FAEnabled(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(enabled ? "2FA đang bật" : "2FA đang tắt", enabled));
    }
}
