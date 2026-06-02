package com.taskoryx.backend.dto.response.gantt;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class GanttResponse {

    private List<GanttTaskItem> tasks;

    // Khoảng thời gian bao phủ toàn bộ task (để FE set viewport mặc định)
    private LocalDate rangeStart;
    private LocalDate rangeEnd;

    // Thống kê nhanh
    private int totalTasks;
    private int overdueCount;
    private int completedCount;
    private int inProgressCount;
    private int noDateCount;     // task không có startDate lẫn dueDate
}
