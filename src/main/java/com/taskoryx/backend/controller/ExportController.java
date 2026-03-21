package com.taskoryx.backend.controller;

import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
@Tag(name = "Export", description = "Xuất báo cáo dữ liệu")
public class ExportController {

    private final ExportService exportService;

    @GetMapping("/projects/{projectId}/tasks/excel")
    @Operation(summary = "Xuất danh sách task của project ra file Excel (.xlsx)")
    public ResponseEntity<byte[]> exportProjectTasksToExcel(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal) {

        byte[] excelData = exportService.exportProjectTasksToExcel(projectId, principal);

        String filename = "tasks-" + LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
    }
}
