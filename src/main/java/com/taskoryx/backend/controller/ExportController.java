package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.request.export.ExportFilter;
import com.taskoryx.backend.entity.Task;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
@Tag(name = "Export", description = "Xuất báo cáo dữ liệu")
public class ExportController {

    private final ExportService exportService;

    @GetMapping("/projects/{projectId}/tasks/excel")
    @Operation(
        summary = "Xuất báo cáo project ra file Excel (.xlsx)",
        description = """
            Xuất báo cáo với tối đa 5 sheet. Có thể chọn sheet nào cần xuất và áp dụng bộ lọc.

            **Sheets có thể chọn** (param `sheets`, phân cách bằng dấu phẩy):
            - `overview`  — Tổng quan KPI dự án
            - `tasks`     — Danh sách task chi tiết (có AutoFilter, freeze header)
            - `members`   — Thống kê công việc theo thành viên
            - `sprints`   — Thống kê theo sprint
            - `overdue`   — Task quá hạn, sắp hạn, chưa giao

            Không truyền `sheets` = xuất đủ 5 sheet.
            """
    )
    public ResponseEntity<byte[]> exportProjectTasksToExcel(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal,

            @Parameter(description = "Sheet cần xuất, phân cách phẩy. VD: overview,tasks,overdue")
            @RequestParam(required = false) Set<String> sheets,

            @Parameter(description = "Lọc theo một hoặc nhiều sprint ID, phân cách phẩy")
            @RequestParam(required = false) List<UUID> sprintIds,

            @Parameter(description = "Lọc theo assignee (user ID)")
            @RequestParam(required = false) UUID assigneeId,

            @Parameter(description = "Deadline/createdAt từ ngày (yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,

            @Parameter(description = "Deadline/createdAt đến ngày (yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,

            @Parameter(description = "Lọc trạng thái, phân cách phẩy. VD: TODO,IN_PROGRESS")
            @RequestParam(required = false) List<Task.TaskStatus> statuses,

            @Parameter(description = "Lọc ưu tiên, phân cách phẩy. VD: HIGH,URGENT")
            @RequestParam(required = false) List<Task.TaskPriority> priorities) {

        ExportFilter filter = new ExportFilter();
        filter.setSheets(sheets);
        filter.setSprintIds(sprintIds);
        filter.setAssigneeId(assigneeId);
        filter.setDateFrom(dateFrom);
        filter.setDateTo(dateTo);
        filter.setStatuses(statuses);
        filter.setPriorities(priorities);

        byte[] excelData = exportService.exportProjectTasksToExcel(projectId, principal, filter);

        String filename = "bao-cao-" + LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
    }
}
