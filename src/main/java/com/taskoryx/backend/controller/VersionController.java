package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.request.version.CreateVersionRequest;
import com.taskoryx.backend.dto.request.version.UpdateVersionRequest;
import com.taskoryx.backend.dto.response.version.RoadmapVersionItem;
import com.taskoryx.backend.dto.response.version.VersionResponse;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.VersionService;
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

@RestController
@RequiredArgsConstructor
@Tag(name = "Versions", description = "Quản lý version/milestone của project")
public class VersionController {

    private final VersionService versionService;

    @PostMapping("/projects/{projectId}/versions")
    @Operation(summary = "Tạo version mới")
    public ResponseEntity<VersionResponse> createVersion(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateVersionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(versionService.createVersion(projectId, request, principal));
    }

    @GetMapping("/projects/{projectId}/versions")
    @Operation(summary = "Lấy danh sách version của project")
    public ResponseEntity<List<VersionResponse>> getVersions(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(versionService.getVersions(projectId, principal));
    }

    @GetMapping("/versions/{id}")
    @Operation(summary = "Lấy chi tiết version")
    public ResponseEntity<VersionResponse> getVersion(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(versionService.getVersion(id, principal));
    }

    @PutMapping("/versions/{id}")
    @Operation(summary = "Cập nhật version")
    public ResponseEntity<VersionResponse> updateVersion(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVersionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(versionService.updateVersion(id, request, principal));
    }

    @DeleteMapping("/versions/{id}")
    @Operation(summary = "Xóa version")
    public ResponseEntity<Void> deleteVersion(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        versionService.deleteVersion(id, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/projects/{projectId}/roadmap")
    @Operation(summary = "Lấy roadmap của project (tất cả version kèm tasks)")
    public ResponseEntity<List<RoadmapVersionItem>> getRoadmap(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(versionService.getRoadmap(projectId, principal));
    }
}
