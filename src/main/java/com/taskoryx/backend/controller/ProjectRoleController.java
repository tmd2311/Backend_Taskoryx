package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.request.role.ChangeMemberRoleRequest;
import com.taskoryx.backend.dto.request.role.CreateProjectRoleRequest;
import com.taskoryx.backend.dto.request.role.UpdateProjectRoleRequest;
import com.taskoryx.backend.dto.response.ApiResponse;
import com.taskoryx.backend.dto.response.role.ProjectRoleResponse;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.ProjectRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller cho Project Role Management
 *
 * GET    /api/projects/{id}/roles                    - Danh sách roles của project
 * POST   /api/projects/{id}/roles                    - Tạo custom role
 * PATCH  /api/projects/{id}/members/{userId}/role    - Đổi role thành viên
 * PUT    /api/project-roles/{id}                     - Cập nhật role
 * DELETE /api/project-roles/{id}                     - Xoá role
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Project Roles", description = "Quản lý vai trò trong dự án")
public class ProjectRoleController {

    private final ProjectRoleService projectRoleService;

    @GetMapping("/projects/{id}/roles")
    @Operation(summary = "Lấy danh sách custom roles của project")
    public ResponseEntity<ApiResponse<List<ProjectRoleResponse>>> getRoles(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                projectRoleService.getRoles(id, principal)));
    }

    @PostMapping("/projects/{id}/roles")
    @Operation(summary = "Tạo custom role mới cho project (Owner/Admin)")
    public ResponseEntity<ApiResponse<ProjectRoleResponse>> createRole(
            @PathVariable UUID id,
            @Valid @RequestBody CreateProjectRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Tạo vai trò thành công",
                projectRoleService.createRole(id, request, principal)));
    }

    @PatchMapping("/projects/{id}/members/{userId}/role")
    @Operation(summary = "Thay đổi role của thành viên (Owner/Admin)")
    public ResponseEntity<ApiResponse<Void>> changeMemberRole(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeMemberRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        projectRoleService.changeMemberRole(id, userId, request, principal);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật vai trò thành công"));
    }

    @PutMapping("/project-roles/{id}")
    @Operation(summary = "Cập nhật custom role (Owner/Admin)")
    public ResponseEntity<ApiResponse<ProjectRoleResponse>> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật vai trò thành công",
                projectRoleService.updateRole(id, request, principal)));
    }

    @DeleteMapping("/project-roles/{id}")
    @Operation(summary = "Xoá custom role (Owner/Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteRole(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        projectRoleService.deleteRole(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Xoá vai trò thành công"));
    }
}
