package com.taskoryx.backend.dto.request.export;

import com.taskoryx.backend.entity.Task;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Bộ lọc cho API xuất Excel.
 * Tất cả field đều optional — null = không lọc theo trường đó.
 */
@Data
public class ExportFilter {

    /**
     * Danh sách sheet cần xuất.
     * Giá trị hợp lệ: overview, tasks, members, sprints, overdue
     * Null hoặc rỗng = xuất tất cả 5 sheet.
     */
    private Set<String> sheets;

    /** Lọc task thuộc sprint cụ thể. Null = tất cả sprint + backlog. */
    private UUID sprintId;

    /** Lọc task được giao cho thành viên cụ thể. Null = tất cả thành viên. */
    private UUID assigneeId;

    /** Lọc task được tạo/deadline từ ngày này. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;

    /** Lọc task được tạo/deadline đến ngày này. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;

    /**
     * Lọc theo trạng thái. VD: "TODO,IN_PROGRESS"
     * Null = tất cả trạng thái.
     */
    private List<Task.TaskStatus> statuses;

    /**
     * Lọc theo mức ưu tiên. VD: "HIGH,URGENT"
     * Null = tất cả mức ưu tiên.
     */
    private List<Task.TaskPriority> priorities;

    public boolean hasSheet(String name) {
        return sheets == null || sheets.isEmpty() || sheets.contains(name.toLowerCase());
    }

    public static final String SHEET_OVERVIEW = "overview";
    public static final String SHEET_TASKS    = "tasks";
    public static final String SHEET_MEMBERS  = "members";
    public static final String SHEET_SPRINTS  = "sprints";
    public static final String SHEET_OVERDUE  = "overdue";
}
