package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.request.checklist.BulkCreateChecklistRequest;
import com.taskoryx.backend.dto.request.checklist.CreateChecklistItemRequest;
import com.taskoryx.backend.dto.request.checklist.UpdateChecklistItemRequest;
import com.taskoryx.backend.dto.response.ApiResponse;
import com.taskoryx.backend.dto.response.checklist.ChecklistItemResponse;
import com.taskoryx.backend.dto.response.checklist.ChecklistSummaryResponse;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.ChecklistItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller cho Checklist trong Task
 *
 * GET    /api/tasks/{taskId}/checklist        - Lấy checklist của task
 * POST   /api/tasks/{taskId}/checklist        - Thêm một item vào checklist
 * POST   /api/tasks/{taskId}/checklist/bulk   - Thêm nhiều items cùng lúc
 * PUT    /api/checklist/{id}                  - Cập nhật item
 * DELETE /api/checklist/{id}                  - Xóa một item
 * DELETE /api/tasks/{taskId}/checklist        - Xóa toàn bộ checklist của task
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Checklist", description = "Quản lý checklist trong task")
public class ChecklistItemController {

    private final ChecklistItemService checklistItemService;

    @GetMapping("/tasks/{taskId}/checklist")
    @Operation(summary = "Lấy toàn bộ checklist của task")
    public ResponseEntity<ApiResponse<ChecklistSummaryResponse>> getChecklist(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                checklistItemService.getChecklist(taskId, principal)));
    }

    @PostMapping("/tasks/{taskId}/checklist")
    @Operation(summary = "Thêm một item vào checklist")
    public ResponseEntity<ApiResponse<ChecklistItemResponse>> addItem(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateChecklistItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đã thêm item vào checklist",
                        checklistItemService.addItem(taskId, request, principal)));
    }

    @PostMapping("/tasks/{taskId}/checklist/bulk")
    @Operation(summary = "Thêm nhiều items vào checklist cùng lúc")
    public ResponseEntity<ApiResponse<ChecklistSummaryResponse>> bulkAdd(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BulkCreateChecklistRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đã thêm checklist",
                        checklistItemService.bulkAdd(taskId, request, principal)));
    }

    @PutMapping("/checklist/{id}")
    @Operation(summary = "Cập nhật nội dung hoặc trạng thái checklist item")
    public ResponseEntity<ApiResponse<ChecklistItemResponse>> updateItem(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateChecklistItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật checklist item thành công",
                checklistItemService.updateItem(id, request, principal)));
    }

    @DeleteMapping("/checklist/{id}")
    @Operation(summary = "Xóa một item khỏi checklist")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        checklistItemService.deleteItem(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa checklist item"));
    }

    @DeleteMapping("/tasks/{taskId}/checklist")
    @Operation(summary = "Xóa toàn bộ checklist của task")
    public ResponseEntity<ApiResponse<Void>> deleteAllItems(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserPrincipal principal) {
        checklistItemService.deleteAllItems(taskId, principal);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa toàn bộ checklist"));
    }
}
