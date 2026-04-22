package com.taskoryx.backend.dto.request.project;

import com.taskoryx.backend.dto.response.template.TemplateConfigDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateProjectRequest {

    @NotBlank(message = "Tên dự án không được để trống")
    @Size(max = 100, message = "Tên dự án không được quá 100 ký tự")
    private String name;

    private String description;

    @NotBlank(message = "Mã dự án không được để trống")
    @Pattern(regexp = "^[A-Z0-9]{2,10}$", message = "Mã dự án phải gồm 2-10 ký tự in hoa hoặc số")
    private String key;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Màu không hợp lệ (ví dụ: #1976d2)")
    private String color;

    private String icon;
    private String projectType;
    private TemplateConfigDto projectConfig;
    private Boolean isPublic = false;
}
