package com.taskoryx.backend.dto.request.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateBoardRequest {

    @NotBlank(message = "Tên bảng không được để trống")
    @Size(max = 100, message = "Tên bảng không được quá 100 ký tự")
    private String name;

    private String description;
}
