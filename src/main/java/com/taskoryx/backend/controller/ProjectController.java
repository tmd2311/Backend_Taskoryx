package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.request.project.AddMemberRequest;
import com.taskoryx.backend.dto.request.project.CreateProjectRequest;
import com.taskoryx.backend.dto.request.project.UpdateMemberRoleRequest;
import com.taskoryx.backend.dto.request.project.UpdateProjectRequest;
import com.taskoryx.backend.dto.response.ApiResponse;
import com.taskoryx.backend.dto.response.comment.MentionedUserInfo;
import com.taskoryx.backend.dto.response.project.ProjectMemberResponse;
import com.taskoryx.backend.dto.response.project.ProjectResponse;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.ProjectService;
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
 * REST Controller cho Project Management
 *
 * GET    /api/projects                              - Danh sách projects của tôi
 * POST   /api/projects                              - Tạo project mới
 * GET    /api/projects/{id}                         - Chi tiết project
 * PUT    /api/projects/{id}                         - Cập nhật project
 * DELETE /api/projects/{id}                         - Xóa project
 * GET    /api/projects/{id}/members                        - Danh sách thành viên
 * GET    /api/projects/{id}/members/search?keyword=       - Tìm thành viên cho @mention
 * POST   /api/projects/{id}/members                       - Thêm thành viên
 * PUT    /api/projects/{id}/members/{userId}/role         - Cập nhật vai trò
 * DELETE /api/projects/{id}/members/{userId}              - Xóa thành viên
 */
@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Quản lý dự án và thành viên")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    @Operation(summary = "Lấy danh sách dự án của tôi")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getMyProjects(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getMyProjects(principal)));
    }

    @PostMapping
    @Operation(summary = "Tạo dự án mới")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo dự án thành công",
                        projectService.createProject(principal, request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết dự án")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProject(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getProject(id, principal)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin dự án")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProjectRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật dự án thành công",
                projectService.updateProject(id, request, principal)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa dự án")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        projectService.deleteProject(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Xóa dự án thành công"));
    }

    // ========== MEMBERS ==========

    @GetMapping("/{id}/members")
    @Operation(summary = "Lấy danh sách thành viên dự án")
    public ResponseEntity<ApiResponse<List<ProjectMemberResponse>>> getMembers(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getMembers(id, principal)));
    }

    @GetMapping("/{id}/members/search")
    @Operation(summary = "Tìm kiếm thành viên để @mention",
               description = "Trả về thành viên trong project khớp keyword. " +
                             "Dùng cho autocomplete khi gõ @ trong comment.")
    public ResponseEntity<ApiResponse<List<MentionedUserInfo>>> searchMembersForMention(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "") String keyword,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                projectService.searchMembersForMention(id, keyword, principal)));
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Thêm thành viên vào dự án")
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> addMember(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm thành viên thành công",
                        projectService.addMember(id, request, principal)));
    }

    @PutMapping("/{id}/members/{userId}/role")
    @Operation(summary = "Cập nhật vai trò thành viên")
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> updateMemberRole(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật vai trò thành công",
                projectService.updateMemberRole(id, userId, request, principal)));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "Xóa thành viên khỏi dự án")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal) {
        projectService.removeMember(id, userId, principal);
        return ResponseEntity.ok(ApiResponse.success("Xóa thành viên thành công"));
    }
}
