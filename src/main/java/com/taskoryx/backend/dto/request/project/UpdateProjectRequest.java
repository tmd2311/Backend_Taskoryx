package com.taskoryx.backend.dto.request.project;

import com.taskoryx.backend.dto.response.template.TemplateConfigDto;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProjectRequest {

    @Size(max = 100, message = "Tên dự án không được quá 100 ký tự")
    private String name;

    private String description;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Màu không hợp lệ (ví dụ: #1976d2)")
    private String color;

    private String icon;
    private String projectType;
    private TemplateConfigDto projectConfig;
    private Boolean isPublic;
    private Boolean isArchived;
}
