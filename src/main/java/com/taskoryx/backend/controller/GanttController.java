package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.response.gantt.GanttTaskItem;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.GanttService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Gantt", description = "Dữ liệu Gantt chart cho project")
public class GanttController {

    private final GanttService ganttService;

    @GetMapping("/projects/{projectId}/gantt")
    @Operation(summary = "Lấy dữ liệu Gantt chart (tasks có startDate hoặc dueDate)")
    public ResponseEntity<List<GanttTaskItem>> getGanttData(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ganttService.getGanttData(projectId, principal));
    }
}
