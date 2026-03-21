package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.request.template.CreateProjectFromTemplateRequest;
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
}
