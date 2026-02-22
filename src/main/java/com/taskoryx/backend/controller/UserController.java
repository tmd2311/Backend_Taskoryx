package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.request.user.ChangePasswordRequest;
import com.taskoryx.backend.dto.request.user.UpdateProfileRequest;
import com.taskoryx.backend.dto.response.ApiResponse;
import com.taskoryx.backend.dto.response.PagedResponse;
import com.taskoryx.backend.dto.response.user.UserResponse;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller cho User Management
 *
 * GET    /api/users/me              - Lấy thông tin cá nhân
 * PUT    /api/users/me              - Cập nhật thông tin cá nhân
 * PUT    /api/users/me/password     - Đổi mật khẩu
 * GET    /api/users/{id}            - Lấy thông tin user khác
 * GET    /api/users/search          - Tìm kiếm user (để @mention)
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Quản lý thông tin người dùng")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin người dùng hiện tại")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(userService.getCurrentUser(principal)));
    }

    @PutMapping("/me")
    @Operation(summary = "Cập nhật thông tin cá nhân")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công",
                userService.updateProfile(principal, request)));
    }

    @PutMapping("/me/password")
    @Operation(summary = "Đổi mật khẩu")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal, request);
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin người dùng theo ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }

    @GetMapping("/search")
    @Operation(summary = "Tìm kiếm người dùng (để assign, @mention)")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> searchUsers(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = userService.searchUsers(keyword, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(result)));
    }
}
