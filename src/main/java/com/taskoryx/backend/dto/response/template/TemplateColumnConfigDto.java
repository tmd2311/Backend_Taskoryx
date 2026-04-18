package com.taskoryx.backend.dto.response.template;

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
}
