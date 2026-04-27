package com.taskoryx.backend.dto.response.template;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateSprintConfigDto {
    private String name;
    private String goal;
    private int durationWeeks; // số tuần, tính từ ngày tạo project
}
