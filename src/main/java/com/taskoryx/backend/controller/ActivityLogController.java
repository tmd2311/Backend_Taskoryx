package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.response.ApiResponse;
import com.taskoryx.backend.dto.response.PagedResponse;
import com.taskoryx.backend.dto.response.activity.ActivityLogResponse;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.ActivityLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller cho Activity Feed
 *
 * GET /api/projects/{projectId}/activity  - Lịch sử hoạt động của project
 * GET /api/tasks/{taskId}/activity        - Lịch sử hoạt động của task
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Activity Feed", description = "Lịch sử hoạt động dự án")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @GetMapping("/projects/{projectId}/activity")
    @Operation(summary = "Lấy lịch sử hoạt động của project")
    public ResponseEntity<ApiResponse<PagedResponse<ActivityLogResponse>>> getProjectActivityFeed(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = activityLogService.getProjectActivityFeed(projectId, page, size, principal);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(result)));
    }

    @GetMapping("/tasks/{taskId}/activity")
    @Operation(summary = "Lấy lịch sử hoạt động của task")
    public ResponseEntity<ApiResponse<List<ActivityLogResponse>>> getTaskActivityFeed(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                activityLogService.getTaskActivityFeed(taskId, principal)));
    }
}
