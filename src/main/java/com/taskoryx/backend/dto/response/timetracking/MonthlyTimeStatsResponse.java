package com.taskoryx.backend.dto.response.timetracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyTimeStatsResponse {

    private int year;
    private int month;
    private String monthName;
    private BigDecimal totalHours;
    private String formattedHours;
    private int entryCount;
    private int activeDays;
    private List<DailyTimeStatsResponse> days;
}
