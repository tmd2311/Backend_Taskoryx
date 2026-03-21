package com.taskoryx.backend.dto.request.version;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateVersionRequest {

    @NotBlank(message = "Tên version không được để trống")
    @Size(max = 100, message = "Tên version không được quá 100 ký tự")
    private String name;

    private String description;

    private LocalDate dueDate;

    private LocalDate releaseDate;
}
