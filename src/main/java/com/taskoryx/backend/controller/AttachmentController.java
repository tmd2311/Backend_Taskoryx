package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.response.ApiResponse;
import com.taskoryx.backend.dto.response.attachment.AttachmentResponse;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller cho File Attachments
 *
 * GET    /api/tasks/{taskId}/attachments          - Danh sách file đính kèm
 * POST   /api/tasks/{taskId}/attachments          - Upload file
 * DELETE /api/attachments/{id}                   - Xóa file
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Attachments", description = "Upload và quản lý file đính kèm trong task")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping("/tasks/{taskId}/attachments")
    @Operation(summary = "Lấy danh sách file đính kèm của task")
    public ResponseEntity<ApiResponse<List<AttachmentResponse>>> getAttachments(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                attachmentService.getAttachments(taskId, principal)));
    }

    @PostMapping(value = "/tasks/{taskId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file đính kèm vào task (max 10MB)")
    public ResponseEntity<ApiResponse<AttachmentResponse>> uploadAttachment(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Upload file thành công",
                        attachmentService.uploadAttachment(taskId, file, principal)));
    }

    @DeleteMapping("/attachments/{id}")
    @Operation(summary = "Xóa file đính kèm")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        attachmentService.deleteAttachment(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Xóa file thành công"));
    }
}
