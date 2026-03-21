package com.taskoryx.backend.dto.request.category;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateCategoryRequest {

    @Size(max = 100, message = "Tên danh mục không được quá 100 ký tự")
    private String name;

    private UUID defaultAssigneeId;

    private boolean clearDefaultAssignee = false;
}
