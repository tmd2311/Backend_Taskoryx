package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.response.ApiResponse;
import com.taskoryx.backend.dto.response.gantt.GanttResponse;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.GanttService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Gantt", description = "Dữ liệu Gantt chart cho project")
public class GanttController {

    private final GanttService ganttService;

    @GetMapping("/projects/{projectId}/gantt")
    @Operation(
        summary = "Lấy dữ liệu Gantt chart",
        description = """
            Trả danh sách task có đầy đủ thông tin để vẽ Gantt:
            - Hierarchy (parentTaskId, depth) để indent task con
            - Sprint info để nhóm theo sprint
            - percentComplete tính từ giờ thực tế/ước tính (fallback theo status)
            - priorityColor để tô màu thanh Gantt
            - dependencies để vẽ mũi tên liên kết
            - rangeStart/rangeEnd để FE set viewport mặc định
            - Thống kê nhanh: tổng task, quá hạn, hoàn thành, đang làm

            **Filter params (tất cả optional):**
            - `sprintId`: chỉ lấy task thuộc sprint này
            - `assigneeId`: chỉ lấy task của người này
            - `dateFrom` / `dateTo`: lọc theo startDate hoặc dueDate (yyyy-MM-dd)
            """
    )
    public ResponseEntity<ApiResponse<GanttResponse>> getGanttData(
            @PathVariable UUID projectId,

            @Parameter(description = "Lọc theo sprint")
            @RequestParam(required = false) UUID sprintId,

            @Parameter(description = "Lọc theo người thực hiện")
            @RequestParam(required = false) UUID assigneeId,

            @Parameter(description = "Lọc task có ngày >= dateFrom (yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,

            @Parameter(description = "Lọc task có ngày <= dateTo (yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,

            @AuthenticationPrincipal UserPrincipal principal) {

        GanttResponse data = ganttService.getGanttData(
                projectId, sprintId, assigneeId, dateFrom, dateTo, principal);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
