package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.request.admin.*;
import com.taskoryx.backend.dto.response.ApiResponse;
import com.taskoryx.backend.dto.response.PagedResponse;
import com.taskoryx.backend.dto.response.admin.AdminUserResponse;
import com.taskoryx.backend.dto.response.admin.PermissionResponse;
import com.taskoryx.backend.dto.response.admin.RoleResponse;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller cho Admin - Quản lý User, Role, Permission
 * Tất cả endpoint yêu cầu permission ADMIN_ACCESS
 *
 * GET    /api/admin/permissions                    - Danh sách permissions
 * GET    /api/admin/roles                          - Danh sách roles
 * POST   /api/admin/roles                          - Tạo role mới
 * PUT    /api/admin/roles/{id}                     - Sửa role
 * DELETE /api/admin/roles/{id}                     - Xóa role
 * POST   /api/admin/roles/{id}/permissions         - Gán permissions vào role
 * DELETE /api/admin/roles/{id}/permissions         - Bỏ permissions khỏi role
 * GET    /api/admin/users                          - Danh sách user
 * POST   /api/admin/users                          - Tạo user mới
 * GET    /api/admin/users/{id}                     - Xem chi tiết user
 * PATCH  /api/admin/users/{id}/status              - Toggle trạng thái user
 * POST   /api/admin/users/{id}/reset-password      - Reset mật khẩu
 * POST   /api/admin/users/{id}/roles               - Gán role cho user
 * DELETE /api/admin/users/{id}/roles/{roleId}      - Bỏ role khỏi user
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Quản lý người dùng, vai trò và phân quyền (yêu cầu ADMIN_ACCESS)")
@PreAuthorize("hasAuthority('ADMIN_ACCESS')")
public class AdminController {

    private final AdminService adminService;

    // ===================== PERMISSIONS =====================

    @GetMapping("/permissions")
    @Operation(summary = "Lấy danh sách tất cả permissions")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAllPermissions() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllPermissions()));
    }

    // ===================== ROLES =====================

    @GetMapping("/roles")
    @Operation(summary = "Lấy danh sách tất cả roles")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllRoles()));
    }

    @GetMapping("/roles/{id}")
    @Operation(summary = "Lấy chi tiết role theo ID")
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getRoleById(id)));
    }

    @PostMapping("/roles")
    @Operation(summary = "Tạo role mới")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo role thành công", adminService.createRole(request)));
    }

    @PutMapping("/roles/{id}")
    @Operation(summary = "Cập nhật thông tin role")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật role thành công", adminService.updateRole(id, request)));
    }

    @DeleteMapping("/roles/{id}")
    @Operation(summary = "Xóa role (không xóa được system role)")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable UUID id) {
        adminService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa role thành công"));
    }

    @PostMapping("/roles/{id}/permissions")
    @Operation(summary = "Gán thêm permissions vào role")
    public ResponseEntity<ApiResponse<RoleResponse>> addPermissionsToRole(
            @PathVariable UUID id,
            @Valid @RequestBody AssignPermissionsRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Gán permissions thành công",
                adminService.addPermissionsToRole(id, request)));
    }

    @DeleteMapping("/roles/{id}/permissions")
    @Operation(summary = "Bỏ permissions khỏi role")
    public ResponseEntity<ApiResponse<RoleResponse>> removePermissionsFromRole(
            @PathVariable UUID id,
            @Valid @RequestBody AssignPermissionsRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Bỏ permissions thành công",
                adminService.removePermissionsFromRole(id, request)));
    }

    // ===================== USERS =====================

    @GetMapping("/users")
    @Operation(summary = "Lấy danh sách tất cả người dùng")
    public ResponseEntity<ApiResponse<PagedResponse<AdminUserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = adminService.getAllUsers(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(result)));
    }

    @PostMapping("/users")
    @Operation(summary = "Tạo tài khoản mới cho nhân viên")
    public ResponseEntity<ApiResponse<AdminUserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo tài khoản thành công", adminService.createUser(request, principal)));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Lấy chi tiết thông tin user")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getUserById(id)));
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Kích hoạt / Vô hiệu hóa tài khoản")
    public ResponseEntity<ApiResponse<AdminUserResponse>> toggleUserStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(adminService.toggleUserStatus(id, principal)));
    }

    @PostMapping("/users/{id}/reset-password")
    @Operation(summary = "Reset mật khẩu tạm thời cho user")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable UUID id,
            @Valid @RequestBody ResetPasswordRequest request) {
        adminService.resetUserPassword(id, request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Reset mật khẩu thành công"));
    }

    @PostMapping("/users/{id}/roles")
    @Operation(summary = "Gán role cho user")
    public ResponseEntity<ApiResponse<AdminUserResponse>> assignRoleToUser(
            @PathVariable UUID id,
            @Valid @RequestBody AssignRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Gán role thành công",
                adminService.assignRoleToUser(id, request, principal)));
    }

    @DeleteMapping("/users/{id}/roles/{roleId}")
    @Operation(summary = "Bỏ role khỏi user")
    public ResponseEntity<ApiResponse<AdminUserResponse>> removeRoleFromUser(
            @PathVariable UUID id,
            @PathVariable UUID roleId) {
        return ResponseEntity.ok(ApiResponse.success("Bỏ role thành công",
                adminService.removeRoleFromUser(id, roleId)));
    }

    // ===================== INNER DTO =====================

    @Data
    static class ResetPasswordRequest {
        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 8, message = "Mật khẩu tối thiểu 8 ký tự")
        private String newPassword;
    }
}
