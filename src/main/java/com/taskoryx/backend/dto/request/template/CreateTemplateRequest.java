package com.taskoryx.backend.dto.request.template;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTemplateRequest {

    @NotBlank(message = "Tên template không được để trống")
    @Size(max = 255, message = "Tên template tối đa 255 ký tự")
    private String name;

    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    private String description;

    @Size(max = 100, message = "Danh mục tối đa 100 ký tự")
    private String category;

    @Size(max = 10)
    private String icon;

    @Size(max = 7)
    private String color;

    @NotBlank(message = "Cấu hình template không được để trống")
    private String columnsConfig;

    private Boolean isPublic = true;
}
