package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.request.sprint.CreateSprintRequest;
import com.taskoryx.backend.dto.request.sprint.SprintTaskRequest;
import com.taskoryx.backend.dto.request.sprint.UpdateSprintRequest;
import com.taskoryx.backend.dto.response.ApiResponse;
import com.taskoryx.backend.dto.response.sprint.SprintResponse;
import com.taskoryx.backend.dto.response.task.TaskSummaryResponse;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.SprintService;
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
 * REST Controller for Sprint Management
 *
 * POST   /api/projects/{projectId}/sprints      - Tạo sprint mới
 * GET    /api/projects/{projectId}/sprints      - Danh sách sprints trong project
 * GET    /api/sprints/{id}                      - Chi tiết sprint (kèm tasks)
 * PUT    /api/sprints/{id}                      - Cập nhật sprint
 * POST   /api/sprints/{id}/start               - Bắt đầu sprint
 * POST   /api/sprints/{id}/complete            - Hoàn thành sprint
 * DELETE /api/sprints/{id}                     - Xóa sprint (chỉ PLANNED)
 * POST   /api/sprints/{id}/tasks               - Thêm task vào sprint
 * DELETE /api/sprints/{id}/tasks/{taskId}      - Xóa task khỏi sprint
 * GET    /api/sprints/{id}/backlog             - Tasks trong sprint chưa kéo lên board (Sprint Backlog)
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Sprints", description = "Quản lý Sprint - lập kế hoạch và theo dõi tiến độ")
public class SprintController {

    private final SprintService sprintService;

    @PostMapping("/projects/{projectId}/sprints")
    @Operation(summary = "Tạo sprint mới trong project")
    public ResponseEntity<ApiResponse<SprintResponse>> createSprint(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateSprintRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo sprint thành công",
                        sprintService.createSprint(projectId, request, principal)));
    }

    @GetMapping("/projects/{projectId}/sprints")
    @Operation(summary = "Lấy danh sách sprints trong project")
    public ResponseEntity<ApiResponse<List<SprintResponse>>> getSprints(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                sprintService.getSprints(projectId, principal)));
    }

    @GetMapping("/sprints/{id}")
    @Operation(summary = "Lấy chi tiết sprint (kèm danh sách tasks)")
    public ResponseEntity<ApiResponse<SprintResponse>> getSprint(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                sprintService.getSprint(id, principal)));
    }

    @PutMapping("/sprints/{id}")
    @Operation(summary = "Cập nhật thông tin sprint")
    public ResponseEntity<ApiResponse<SprintResponse>> updateSprint(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateSprintRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật sprint thành công",
                sprintService.updateSprint(id, request, principal)));
    }

    @PostMapping("/sprints/{id}/start")
    @Operation(summary = "Bắt đầu sprint (PLANNED → ACTIVE)")
    public ResponseEntity<ApiResponse<SprintResponse>> startSprint(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Sprint đã được bắt đầu",
                sprintService.startSprint(id, principal)));
    }

    @PostMapping("/sprints/{id}/complete")
    @Operation(summary = "Hoàn thành sprint (ACTIVE → COMPLETED)")
    public ResponseEntity<ApiResponse<SprintResponse>> completeSprint(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Sprint đã hoàn thành",
                sprintService.completeSprint(id, principal)));
    }

    @DeleteMapping("/sprints/{id}")
    @Operation(summary = "Xóa sprint (chỉ sprint ở trạng thái PLANNED)")
    public ResponseEntity<ApiResponse<Void>> deleteSprint(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        sprintService.deleteSprint(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Xóa sprint thành công", null));
    }

    @PostMapping("/sprints/{id}/tasks")
    @Operation(summary = "Thêm task vào sprint")
    public ResponseEntity<ApiResponse<SprintResponse>> addTaskToSprint(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SprintTaskRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Thêm task vào sprint thành công",
                sprintService.addTaskToSprint(id, request.getTaskId(), principal)));
    }

    @DeleteMapping("/sprints/{id}/tasks/{taskId}")
    @Operation(summary = "Xóa task khỏi sprint")
    public ResponseEntity<ApiResponse<SprintResponse>> removeTaskFromSprint(
            @PathVariable UUID id,
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Xóa task khỏi sprint thành công",
                sprintService.removeTaskFromSprint(id, taskId, principal)));
    }

    @GetMapping("/sprints/{id}/backlog")
    @Operation(summary = "Lấy Sprint Backlog - tasks trong sprint chưa được kéo lên board")
    public ResponseEntity<ApiResponse<List<TaskSummaryResponse>>> getSprintBacklog(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                sprintService.getSprintBacklog(id, principal)));
    }
}
