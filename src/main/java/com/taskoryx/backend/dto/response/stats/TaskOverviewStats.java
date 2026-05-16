package com.taskoryx.backend.dto.response.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskOverviewStats {

    private long total;
    private long todo;
    private long inProgress;
    private long inReview;
    private long resolved;
    private long done;
    private long cancelled;
    private long overdue;

    /** % hoàn thành = (done + resolved + cancelled) / total * 100 */
    private double completionRate;

    // Theo priority
    private long low;
    private long medium;
    private long high;
    private long urgent;

    // Không có assignee
    private long unassigned;
}
