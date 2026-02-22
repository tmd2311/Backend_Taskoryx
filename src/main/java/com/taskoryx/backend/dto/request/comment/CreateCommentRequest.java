package com.taskoryx.backend.dto.request.comment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateCommentRequest {

    @NotBlank(message = "Nội dung bình luận không được để trống")
    private String content;

    private UUID parentId; // null nếu là comment gốc, có giá trị nếu là reply
}
