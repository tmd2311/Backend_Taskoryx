package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.response.ApiResponse;
import com.taskoryx.backend.dto.response.task.TaskSummaryResponse;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller cho Search & Filter
 *
 * GET /api/search                                  - Tìm kiếm toàn cục
 * GET /api/projects/{projectId}/tasks/search       - Tìm kiếm task trong project
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Search", description = "Tìm kiếm và lọc dữ liệu")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/search")
    @Operation(summary = "Tìm kiếm toàn cục (projects, users)")
    public ResponseEntity<ApiResponse<SearchService.GlobalSearchResult>> globalSearch(
            @RequestParam String q,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(searchService.search(q, principal)));
    }

    @GetMapping("/projects/{projectId}/tasks/search")
    @Operation(summary = "Tìm kiếm task trong project theo tiêu đề hoặc mô tả")
    public ResponseEntity<ApiResponse<List<TaskSummaryResponse>>> searchTasksInProject(
            @PathVariable UUID projectId,
            @RequestParam String q,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                searchService.searchTasksInProject(q, projectId, principal)));
    }
}
