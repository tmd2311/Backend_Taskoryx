package com.taskoryx.backend.dto.request.task;

import com.taskoryx.backend.entity.Task;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request để lọc và tìm kiếm task
 */
@Data
public class TaskFilterRequest {

    private String keyword;
    private UUID columnId;
    private UUID sprintId;
    private UUID assigneeId;
    private List<Task.TaskPriority> priorities;
    private List<UUID> labelIds;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDateTo;

    private Boolean overdue;
    private Boolean completed;

    // Pagination
    private int page = 0;
    private int size = 20;
    private String sortBy = "createdAt";
    private String sortDir = "desc";
}
