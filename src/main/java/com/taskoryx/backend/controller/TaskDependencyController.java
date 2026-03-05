package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.request.task.AddDependencyRequest;
import com.taskoryx.backend.dto.response.ApiResponse;
import com.taskoryx.backend.dto.response.task.TaskDependencyResponse;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.TaskDependencyService;
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
 * GET    /api/tasks/{taskId}/dependencies             - Danh sách dependencies của task
 * POST   /api/tasks/{taskId}/dependencies             - Thêm dependency (có check circular)
 * DELETE /api/tasks/{taskId}/dependencies/{depId}     - Xóa dependency
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/tasks/{taskId}/dependencies")
@Tag(name = "Task Dependencies", description = "Quản lý phụ thuộc giữa các task")
public class TaskDependencyController {

    private final TaskDependencyService dependencyService;

    @GetMapping
    @Operation(summary = "Lấy danh sách dependencies của task")
    public ResponseEntity<ApiResponse<List<TaskDependencyResponse>>> getDependencies(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                dependencyService.getDependencies(taskId, principal)));
    }

    @PostMapping
    @Operation(summary = "Thêm dependency cho task (tự động kiểm tra circular dependency)")
    public ResponseEntity<ApiResponse<TaskDependencyResponse>> addDependency(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddDependencyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm dependency thành công",
                        dependencyService.addDependency(taskId, request, principal)));
    }

    @DeleteMapping("/{dependencyId}")
    @Operation(summary = "Xóa dependency")
    public ResponseEntity<ApiResponse<Void>> removeDependency(
            @PathVariable UUID taskId,
            @PathVariable UUID dependencyId,
            @AuthenticationPrincipal UserPrincipal principal) {
        dependencyService.removeDependency(taskId, dependencyId, principal);
        return ResponseEntity.ok(ApiResponse.success("Xóa dependency thành công", null));
    }
}
