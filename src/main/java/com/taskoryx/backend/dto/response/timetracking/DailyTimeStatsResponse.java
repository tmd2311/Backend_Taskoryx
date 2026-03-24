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
public class DailyTimeStatsResponse {

    private LocalDate date;
    private String dayOfWeek;
    private BigDecimal totalHours;
    private String formattedHours;
    private int entryCount;
    private List<TimeTrackingResponse> entries;
}
