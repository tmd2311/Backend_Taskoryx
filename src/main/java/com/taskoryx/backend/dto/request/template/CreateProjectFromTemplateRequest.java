package com.taskoryx.backend.dto.request.template;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateProjectFromTemplateRequest {
    @NotBlank
    @Size(max = 255)
    private String name;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{2,10}$", message = "Mã dự án phải là 2-10 ký tự viết hoa")
    private String key;

    private String description;
    private String color;
    private Boolean isPublic;
}
