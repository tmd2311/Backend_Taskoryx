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
public class WeeklyTimeStatsResponse {

    private int year;
    private int weekOfYear;
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private String weekLabel;
    private BigDecimal totalHours;
    private String formattedHours;
    private int entryCount;
    private List<DailyTimeStatsResponse> days;
}
