package com.taskoryx.backend.dto.response.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveSprintStats {

    private UUID sprintId;
    private String sprintName;
    private LocalDate startDate;
    private LocalDate endDate;

    /** Số ngày còn lại (âm = đã quá hạn sprint) */
    private long daysRemaining;

    private long totalTasks;
    private long doneTasks;
    private long inProgressTasks;
    private long todoTasks;

    /** % hoàn thành = doneTasks / totalTasks * 100 */
    private double completionRate;
}
