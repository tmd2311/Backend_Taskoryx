package com.taskoryx.backend.dto.response.template;

import com.taskoryx.backend.entity.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateColumnConfigDto {
    private String name;
    private String color;
    private Boolean isCompleted;
    private Task.TaskStatus mappedStatus;
    private Integer taskLimit;
}
