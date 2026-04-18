package com.taskoryx.backend.service;

import com.taskoryx.backend.entity.Task;
import com.taskoryx.backend.entity.ProjectPermission;
import com.taskoryx.backend.repository.TaskRepository;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final TaskRepository taskRepository;
    private final ProjectAuthorizationService projectAuthorizationService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Transactional(readOnly = true)
    public byte[] exportProjectTasksToExcel(UUID projectId, UserPrincipal principal) {
        projectAuthorizationService.requirePermission(projectId, principal.getId(), ProjectPermission.REPORT_VIEW);

        List<Task> tasks = taskRepository.findByProjectId(projectId, PageRequest.of(0, 10000)).getContent();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Tasks");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            // Date style
            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/mm/yyyy"));

            // Create header row
            String[] headers = {
                "Task Key", "Tiêu đề", "Mô tả", "Trạng thái", "Ưu tiên",
                "Người thực hiện", "Người báo cáo", "Ngày bắt đầu", "Deadline",
                "Giờ ước tính", "Giờ thực tế", "Ngày tạo", "Ngày hoàn thành"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }
            sheet.setColumnWidth(1, 8000); // Title wider
            sheet.setColumnWidth(2, 10000); // Description wider

            // Data rows
            int rowNum = 1;
            for (Task task : tasks) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(task.getTaskKey() != null ? task.getTaskKey() : "");
                row.createCell(1).setCellValue(task.getTitle());
                row.createCell(2).setCellValue(task.getDescription() != null ? task.getDescription() : "");
                row.createCell(3).setCellValue(task.getStatus().name());
                row.createCell(4).setCellValue(task.getPriority().name());
                row.createCell(5).setCellValue(task.getAssignee() != null ? task.getAssignee().getFullName() : "");
                row.createCell(6).setCellValue(task.getReporter() != null ? task.getReporter().getFullName() : "");
                row.createCell(7).setCellValue(task.getStartDate() != null ? task.getStartDate().format(DATE_FMT) : "");
                row.createCell(8).setCellValue(task.getDueDate() != null ? task.getDueDate().format(DATE_FMT) : "");
                row.createCell(9).setCellValue(task.getEstimatedHours() != null ? task.getEstimatedHours().doubleValue() : 0);
                row.createCell(10).setCellValue(task.getActualHours() != null ? task.getActualHours().doubleValue() : 0);
                row.createCell(11).setCellValue(task.getCreatedAt() != null ? task.getCreatedAt().format(DATETIME_FMT) : "");
                row.createCell(12).setCellValue(task.getCompletedAt() != null ? task.getCompletedAt().format(DATETIME_FMT) : "");
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo file Excel", e);
        }
    }
}
