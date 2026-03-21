package com.taskoryx.backend.dto.response.template;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskoryx.backend.entity.ProjectTemplate;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private List<Map<String, Object>> columnsConfig;
    private boolean isPublic;
    private LocalDateTime createdAt;

    public static ProjectTemplateResponse from(ProjectTemplate template) {
        List<Map<String, Object>> columns = null;
        if (template.getColumnsConfig() != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                columns = mapper.readValue(template.getColumnsConfig(),
                    new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception e) {
                columns = List.of();
            }
        }
        return ProjectTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .category(template.getCategory())
                .icon(template.getIcon())
                .color(template.getColor())
                .columnsConfig(columns)
                .isPublic(template.isPublic())
                .createdAt(template.getCreatedAt())
                .build();
    }
}
