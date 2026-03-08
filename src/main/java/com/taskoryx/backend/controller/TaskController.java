package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.request.task.CreateTaskRequest;
import com.taskoryx.backend.dto.request.task.MoveTaskRequest;
import com.taskoryx.backend.dto.request.task.TaskFilterRequest;
import com.taskoryx.backend.dto.request.task.UpdateTaskRequest;
import com.taskoryx.backend.dto.request.task.UpdateTaskStatusRequest;
import com.taskoryx.backend.dto.response.ApiResponse;
import com.taskoryx.backend.dto.response.PagedResponse;
import com.taskoryx.backend.dto.response.task.TaskResponse;
import com.taskoryx.backend.dto.response.task.TaskSummaryResponse;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.TaskService;
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
 * REST Controller cho Task Management
 *
 * POST   /api/projects/{projectId}/tasks           - Tạo task mới (boardId/columnId optional → backlog)
 * GET    /api/projects/{projectId}/tasks           - Danh sách tasks (có lọc/tìm kiếm)
 * GET    /api/projects/{projectId}/backlog         - Danh sách tasks trong Backlog
 * GET    /api/tasks/{id}                           - Chi tiết task
 * PUT    /api/tasks/{id}                           - Cập nhật task
 * PATCH  /api/tasks/{id}/status                    - Cập nhật trạng thái task
 * PATCH  /api/tasks/{id}/move                      - Di chuyển task (drag & drop, targetColumnId null → backlog)
 * DELETE /api/tasks/{id}                           - Xóa task
 * GET    /api/tasks/my                             - Tasks được giao cho tôi
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Quản lý task - tạo, sửa, xóa, assign, di chuyển")
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/projects/{projectId}/tasks")
    @Operation(summary = "Tạo task mới trong project")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo task thành công",
                        taskService.createTask(projectId, request, principal)));
    }

    @GetMapping("/projects/{projectId}/tasks")
    @Operation(summary = "Lấy danh sách tasks trong project (hỗ trợ lọc và tìm kiếm)")
    public ResponseEntity<ApiResponse<PagedResponse<TaskSummaryResponse>>> getTasksByProject(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal,
            @ModelAttribute TaskFilterRequest filter) {
        var result = taskService.getTasksByProject(projectId, filter, principal);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(result)));
    }

    @GetMapping("/projects/{projectId}/backlog")
    @Operation(summary = "Lấy danh sách tasks trong Backlog (chưa được đưa vào board nào)")
    public ResponseEntity<ApiResponse<List<TaskSummaryResponse>>> getBacklog(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getBacklog(projectId, principal)));
    }

    @GetMapping("/tasks/{id}")
    @Operation(summary = "Lấy chi tiết task")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTask(id, principal)));
    }

    @PutMapping("/tasks/{id}")
    @Operation(summary = "Cập nhật task (tiêu đề, mô tả, priority, deadline, assignee, label)")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật task thành công",
                taskService.updateTask(id, request, principal)));
    }

    @PatchMapping("/tasks/{id}/status")
    @Operation(summary = "Cập nhật trạng thái task (TODO / IN_PROGRESS / IN_REVIEW / RESOLVED / DONE / CANCELLED)")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTaskStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công",
                taskService.updateTaskStatus(id, request, principal)));
    }

    @PatchMapping("/tasks/{id}/move")
    @Operation(summary = "Di chuyển task sang cột khác hoặc thay đổi vị trí (drag & drop Kanban)")
    public ResponseEntity<ApiResponse<TaskResponse>> moveTask(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MoveTaskRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Di chuyển task thành công",
                taskService.moveTask(id, request, principal)));
    }

    @DeleteMapping("/tasks/{id}")
    @Operation(summary = "Xóa task")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        taskService.deleteTask(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Xóa task thành công"));
    }

    @GetMapping("/tasks/my")
    @Operation(summary = "Lấy danh sách tasks được giao cho tôi")
    public ResponseEntity<ApiResponse<List<TaskSummaryResponse>>> getMyTasks(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getMyTasks(principal)));
    }
}
