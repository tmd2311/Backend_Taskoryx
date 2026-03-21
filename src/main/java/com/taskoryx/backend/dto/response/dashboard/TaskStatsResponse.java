package com.taskoryx.backend.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatsResponse {

    private long total;
    private long todo;
    private long inProgress;
    private long inReview;
    private long resolved;
    private long done;
    private long cancelled;
    private long overdue;
    private long completedThisWeek;
}
