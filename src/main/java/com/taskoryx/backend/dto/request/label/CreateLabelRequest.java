package com.taskoryx.backend.dto.request.label;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateLabelRequest {

    @NotBlank(message = "Tên nhãn không được để trống")
    @Size(max = 50, message = "Tên nhãn không được quá 50 ký tự")
    private String name;

    @NotBlank(message = "Màu nhãn không được để trống")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Màu không hợp lệ (ví dụ: #FF5733)")
    private String color;

    @Size(max = 200, message = "Mô tả không được quá 200 ký tự")
    private String description;
}
