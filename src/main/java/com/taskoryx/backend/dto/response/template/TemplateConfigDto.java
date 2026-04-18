package com.taskoryx.backend.dto.response.template;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateConfigDto {
    private String boardType; // "KANBAN" | "SCRUM"
    private List<TemplateColumnConfigDto> columns;
    private List<String> taskFields; // e.g. ["priority","dueDate","estimatedHours","assignee"]
    private EvaluationConfigDto evaluationConfig;
}
