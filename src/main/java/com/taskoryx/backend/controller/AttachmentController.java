package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.response.ApiResponse;
import com.taskoryx.backend.dto.response.PagedResponse;
import com.taskoryx.backend.dto.response.attachment.AttachmentResponse;
import com.taskoryx.backend.dto.response.attachment.AttachmentStatsResponse;
import com.taskoryx.backend.entity.FileCategory;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller cho File Attachments
 *
 * GET    /api/tasks/{taskId}/attachments               - Danh sách file đính kèm (hỗ trợ ?category=)
 * POST   /api/tasks/{taskId}/attachments               - Upload file
 * DELETE /api/attachments/{id}                         - Xóa file
 * GET    /api/projects/{projectId}/attachments         - Toàn bộ file trong project (phân trang, ?category=)
 * GET    /api/tasks/{taskId}/attachments/stats         - Thống kê file theo category cho task
 * GET    /api/projects/{projectId}/attachments/stats   - Thống kê file theo category cho project
 * GET    /api/attachments/{id}/download                - Download file (Content-Disposition: attachment)
 * GET    /api/attachments/{id}/inline                  - Xem trực tiếp (Content-Disposition: inline)
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Attachments", description = "Upload, phân loại và quản lý file đính kèm trong task")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping("/tasks/{taskId}/attachments")
    @Operation(summary = "Lấy danh sách file đính kèm của task (hỗ trợ lọc theo ?category=IMAGE|DOCUMENT|...)")
    public ResponseEntity<ApiResponse<List<AttachmentResponse>>> getAttachments(
            @PathVariable UUID taskId,
            @Parameter(description = "Lọc theo danh mục: IMAGE, DOCUMENT, SPREADSHEET, PRESENTATION, VIDEO, AUDIO, ARCHIVE, CODE, OTHER")
            @RequestParam(required = false) String category,
            @AuthenticationPrincipal UserPrincipal principal) {
        FileCategory fileCategory = parseCategory(category);
        return ResponseEntity.ok(ApiResponse.success(
                attachmentService.getAttachments(taskId, fileCategory, principal)));
    }

    @PostMapping(value = "/tasks/{taskId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file đính kèm vào task. Truyền thêm ?commentId= để gắn file vào comment cụ thể.")
    public ResponseEntity<ApiResponse<AttachmentResponse>> uploadAttachment(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) UUID commentId) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Upload file thành công",
                        attachmentService.uploadAttachment(taskId, file, commentId, principal)));
    }

    @GetMapping("/comments/{commentId}/attachments")
    @Operation(summary = "Lấy danh sách file đính kèm của một comment")
    public ResponseEntity<ApiResponse<List<AttachmentResponse>>> getCommentAttachments(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                attachmentService.getCommentAttachments(commentId, principal)));
    }

    @DeleteMapping("/attachments/{id}")
    @Operation(summary = "Xóa file đính kèm")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        attachmentService.deleteAttachment(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Xóa file thành công"));
    }

    @GetMapping("/projects/{projectId}/attachments")
    @Operation(summary = "Lấy toàn bộ file đính kèm trong project (phân trang, hỗ trợ ?category=)")
    public ResponseEntity<ApiResponse<PagedResponse<AttachmentResponse>>> getProjectAttachments(
            @PathVariable UUID projectId,
            @Parameter(description = "Lọc theo danh mục: IMAGE, DOCUMENT, SPREADSHEET, PRESENTATION, VIDEO, AUDIO, ARCHIVE, CODE, OTHER")
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        FileCategory fileCategory = parseCategory(category);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(
                PagedResponse.of(attachmentService.getProjectAttachments(projectId, fileCategory, pageable, principal))));
    }

    @GetMapping("/tasks/{taskId}/attachments/stats")
    @Operation(summary = "Thống kê số lượng file đính kèm theo danh mục cho task")
    public ResponseEntity<ApiResponse<AttachmentStatsResponse>> getTaskStats(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                attachmentService.getTaskStats(taskId, principal)));
    }

    @GetMapping("/projects/{projectId}/attachments/stats")
    @Operation(summary = "Thống kê số lượng file đính kèm theo danh mục cho toàn project")
    public ResponseEntity<ApiResponse<AttachmentStatsResponse>> getProjectStats(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                attachmentService.getProjectStats(projectId, principal)));
    }

    @GetMapping("/attachments/{id}/download")
    @Operation(summary = "Download file đính kèm (Content-Disposition: attachment)")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return attachmentService.serveFile(id, false, principal);
    }

    @GetMapping("/attachments/{id}/inline")
    @Operation(summary = "Xem file trực tiếp trong trình duyệt (Content-Disposition: inline, phù hợp cho ảnh/PDF)")
    public ResponseEntity<Resource> inlineAttachment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return attachmentService.serveFile(id, true, principal);
    }

    /**
     * Chuyển đổi chuỗi category sang FileCategory enum (case-insensitive).
     * Ném BadRequestException nếu giá trị không hợp lệ.
     */
    private FileCategory parseCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        try {
            return FileCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            String validValues = Arrays.stream(FileCategory.values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new BadRequestException("Danh mục không hợp lệ: '" + category
                    + "'. Giá trị hợp lệ: " + validValues);
        }
    }
}
