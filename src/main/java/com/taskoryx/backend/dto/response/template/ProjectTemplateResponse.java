package com.taskoryx.backend.dto.response.template;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskoryx.backend.entity.ProjectTemplate;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectTemplateResponse {
    private UUID id;
    private String name;
    private String description;
    private String category;
    private String icon;
    private String color;
    private TemplateConfigDto config;
    private boolean isPublic;
    private LocalDateTime createdAt;

    public static ProjectTemplateResponse from(ProjectTemplate template) {
        TemplateConfigDto config = null;
        if (template.getColumnsConfig() != null) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                // Try new full-config format first
                config = mapper.readValue(template.getColumnsConfig(), TemplateConfigDto.class);
            } catch (Exception e) {
                try {
                    // Fallback: old format was a plain array of columns
                    List<TemplateColumnConfigDto> cols = mapper.readValue(
                            template.getColumnsConfig(),
                            new TypeReference<List<TemplateColumnConfigDto>>() {});
                    config = TemplateConfigDto.builder().columns(cols).build();
                } catch (Exception e2) {
                    config = TemplateConfigDto.builder().build();
                }
            }
        }
        return ProjectTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .category(template.getCategory())
                .icon(template.getIcon())
                .color(template.getColor())
                .config(config)
                .isPublic(template.isPublic())
                .createdAt(template.getCreatedAt())
                .build();
    }
}
