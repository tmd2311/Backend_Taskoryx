package com.taskoryx.backend.dto.request.version;

import com.taskoryx.backend.entity.Version;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateVersionRequest {

    @Size(max = 100, message = "Tên version không được quá 100 ký tự")
    private String name;

    private String description;

    private Version.VersionStatus status;

    private LocalDate dueDate;

    private LocalDate releaseDate;
}
