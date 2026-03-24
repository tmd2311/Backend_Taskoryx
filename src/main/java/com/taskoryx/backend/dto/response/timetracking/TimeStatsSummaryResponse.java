package com.taskoryx.backend.dto.response.timetracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeStatsSummaryResponse {

    private LocalDate startDate;
    private LocalDate endDate;

    private BigDecimal totalHours;
    private String formattedTotalHours;
    private int totalEntries;

    /** Số ngày có ghi nhận thời gian */
    private int activeDays;

    /** Trung bình số giờ/ngày (tính trên activeDays) */
    private BigDecimal avgHoursPerActiveDay;

    /** Trung bình số giờ/ngày (tính trên tổng số ngày trong khoảng) */
    private BigDecimal avgHoursPerDay;

    private List<ProjectTimeStatsResponse> byProject;
    private List<DailyTimeStatsResponse> byDay;
}
