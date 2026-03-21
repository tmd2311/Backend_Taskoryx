package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.response.watcher.WatcherResponse;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.TaskWatcherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/tasks/{taskId}/watchers")
@RequiredArgsConstructor
@Tag(name = "Task Watchers", description = "Theo dõi task để nhận thông báo")
public class TaskWatcherController {

    private final TaskWatcherService watcherService;

    @PostMapping
    @Operation(summary = "Theo dõi task (watch)")
    public ResponseEntity<WatcherResponse> watchTask(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(watcherService.watchTask(taskId, principal));
    }

    @DeleteMapping
    @Operation(summary = "Bỏ theo dõi task (unwatch)")
    public ResponseEntity<Void> unwatchTask(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserPrincipal principal) {
        watcherService.unwatchTask(taskId, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách người đang theo dõi task")
    public ResponseEntity<List<WatcherResponse>> getWatchers(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(watcherService.getWatchers(taskId, principal));
    }

    @GetMapping("/status")
    @Operation(summary = "Kiểm tra bạn có đang theo dõi task không")
    public ResponseEntity<Map<String, Boolean>> isWatching(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(Map.of("watching", watcherService.isWatching(taskId, principal)));
    }
}
