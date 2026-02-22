package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.request.comment.CreateCommentRequest;
import com.taskoryx.backend.dto.request.comment.UpdateCommentRequest;
import com.taskoryx.backend.dto.response.ApiResponse;
import com.taskoryx.backend.dto.response.comment.CommentResponse;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.CommentService;
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
 * REST Controller cho Comments & Mentions
 *
 * GET    /api/tasks/{taskId}/comments     - Lấy danh sách comments
 * POST   /api/tasks/{taskId}/comments     - Thêm comment (hỗ trợ @mention)
 * PUT    /api/comments/{id}               - Sửa comment
 * DELETE /api/comments/{id}              - Xóa comment
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Bình luận và @mention trong task")
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/tasks/{taskId}/comments")
    @Operation(summary = "Lấy danh sách bình luận của task")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                commentService.getComments(taskId, principal)));
    }

    @PostMapping("/tasks/{taskId}/comments")
    @Operation(summary = "Thêm bình luận (hỗ trợ @username để mention)")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đã thêm bình luận",
                        commentService.createComment(taskId, request, principal)));
    }

    @PutMapping("/comments/{id}")
    @Operation(summary = "Chỉnh sửa bình luận")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateCommentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật bình luận thành công",
                commentService.updateComment(id, request, principal)));
    }

    @DeleteMapping("/comments/{id}")
    @Operation(summary = "Xóa bình luận")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        commentService.deleteComment(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Xóa bình luận thành công"));
    }
}
