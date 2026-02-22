package com.taskoryx.backend.dto.request.board;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateColumnRequest {

    @NotBlank(message = "Tên cột không được để trống")
    @Size(max = 50, message = "Tên cột không được quá 50 ký tự")
    private String name;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Màu không hợp lệ")
    private String color;

    private Boolean isCompleted = false;

    @Min(value = 1, message = "Giới hạn task phải lớn hơn 0")
    private Integer taskLimit;
}
