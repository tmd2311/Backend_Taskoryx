package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.request.label.CreateLabelRequest;
import com.taskoryx.backend.dto.response.ApiResponse;
import com.taskoryx.backend.dto.response.label.LabelResponse;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.LabelService;
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
 * REST Controller cho Labels
 *
 * GET    /api/projects/{projectId}/labels     - Danh sách nhãn
 * POST   /api/projects/{projectId}/labels     - Tạo nhãn mới
 * DELETE /api/labels/{id}                     - Xóa nhãn
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Labels", description = "Quản lý nhãn/tag trong dự án")
public class LabelController {

    private final LabelService labelService;

    @GetMapping("/projects/{projectId}/labels")
    @Operation(summary = "Lấy danh sách nhãn trong project")
    public ResponseEntity<ApiResponse<List<LabelResponse>>> getLabels(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(labelService.getLabels(projectId, principal)));
    }

    @PostMapping("/projects/{projectId}/labels")
    @Operation(summary = "Tạo nhãn mới trong project")
    public ResponseEntity<ApiResponse<LabelResponse>> createLabel(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateLabelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo nhãn thành công",
                        labelService.createLabel(projectId, request, principal)));
    }

    @DeleteMapping("/labels/{id}")
    @Operation(summary = "Xóa nhãn")
    public ResponseEntity<ApiResponse<Void>> deleteLabel(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        labelService.deleteLabel(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Xóa nhãn thành công"));
    }
}
