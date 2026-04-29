package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.request.template.CreateProjectFromTemplateRequest;
import com.taskoryx.backend.dto.request.template.CreateTemplateRequest;
import com.taskoryx.backend.dto.response.ApiResponse;
import com.taskoryx.backend.dto.response.project.ProjectResponse;
import com.taskoryx.backend.dto.response.template.ProjectTemplateResponse;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.ProjectTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/templates")
@RequiredArgsConstructor
@Tag(name = "Project Templates", description = "Template dự án - tạo nhanh project từ mẫu có sẵn")
public class ProjectTemplateController {

    private final ProjectTemplateService templateService;

    @GetMapping
    @Operation(summary = "Lấy danh sách template dự án")
    public ResponseEntity<ApiResponse<List<ProjectTemplateResponse>>> getTemplates(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(templateService.getTemplates(principal)));
    }

    @GetMapping("/public")
    @Operation(summary = "Lấy danh sách template công khai (không cần đăng nhập)")
    public ResponseEntity<ApiResponse<List<ProjectTemplateResponse>>> getPublicTemplates() {
        return ResponseEntity.ok(ApiResponse.success(templateService.getPublicTemplates()));
    }

    @GetMapping("/{templateId}")
    @Operation(summary = "Xem chi tiết một template")
    public ResponseEntity<ApiResponse<ProjectTemplateResponse>> getTemplateById(
            @PathVariable UUID templateId) {
        return ResponseEntity.ok(ApiResponse.success(templateService.getTemplateById(templateId)));
    }

    @PostMapping("/{templateId}/use")
    @Operation(summary = "Tạo dự án mới từ template")
    public ResponseEntity<ApiResponse<ProjectResponse>> createFromTemplate(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateProjectFromTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo dự án từ template thành công",
                        templateService.createProjectFromTemplate(templateId, request, principal)));
    }

    // ── Admin-only endpoints ─────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAuthority('TEMPLATE_MANAGE')")
    @Operation(summary = "[TEMPLATE_MANAGE] Tạo template mới")
    public ResponseEntity<ApiResponse<ProjectTemplateResponse>> createTemplate(
            @Valid @RequestBody CreateTemplateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo template thành công",
                        templateService.createTemplate(request, principal)));
    }

    @PutMapping("/{templateId}")
    @PreAuthorize("hasAuthority('TEMPLATE_MANAGE')")
    @Operation(summary = "[TEMPLATE_MANAGE] Cập nhật template")
    public ResponseEntity<ApiResponse<ProjectTemplateResponse>> updateTemplate(
            @PathVariable UUID templateId,
            @Valid @RequestBody CreateTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật template thành công",
                templateService.updateTemplate(templateId, request)));
    }

    @DeleteMapping("/{templateId}")
    @PreAuthorize("hasAuthority('TEMPLATE_MANAGE')")
    @Operation(summary = "[TEMPLATE_MANAGE] Xóa template")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable UUID templateId) {
        templateService.deleteTemplate(templateId);
        return ResponseEntity.ok(ApiResponse.success("Xóa template thành công", null));
    }
}
