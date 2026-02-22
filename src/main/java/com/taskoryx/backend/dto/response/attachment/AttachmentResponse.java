package com.taskoryx.backend.dto.response.attachment;

import com.taskoryx.backend.entity.Attachment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponse {

    private UUID id;
    private UUID taskId;
    private UUID uploadedById;
    private String uploadedByName;
    private String fileName;
    private Long fileSize;
    private String formattedFileSize;
    private String fileType;
    private String fileUrl;
    private boolean isImage;
    private LocalDateTime createdAt;

    public static AttachmentResponse from(Attachment attachment) {
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .taskId(attachment.getTask().getId())
                .uploadedById(attachment.getUploadedBy().getId())
                .uploadedByName(attachment.getUploadedBy().getFullName())
                .fileName(attachment.getFileName())
                .fileSize(attachment.getFileSize())
                .formattedFileSize(attachment.getFormattedFileSize())
                .fileType(attachment.getFileType())
                .fileUrl(attachment.getFileUrl())
                .isImage(attachment.isImage())
                .createdAt(attachment.getCreatedAt())
                .build();
    }
}
