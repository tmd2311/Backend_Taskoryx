package com.taskoryx.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity class representing file Attachment
 * Maps to 'attachments' table in database
 */
@Entity
@Table(name = "attachments", indexes = {
    @Index(name = "idx_attachments_task", columnList = "task_id"),
    @Index(name = "idx_attachments_user", columnList = "uploaded_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false, foreignKey = @ForeignKey(name = "fk_attachments_task"))
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false, foreignKey = @ForeignKey(name = "fk_attachments_user"))
    private User uploadedBy;

    @NotBlank(message = "{attachment.filename.required}")
    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @NotBlank(message = "{attachment.filetype.required}")
    @Column(name = "file_type", nullable = false, length = 100)
    private String fileType;

    @NotBlank(message = "{attachment.fileurl.required}")
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @NotBlank(message = "{attachment.storagepath.required}")
    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Get file size in human-readable format
     */
    @Transient
    public String getFormattedFileSize() {
        if (fileSize == null) return "0 B";

        long bytes = fileSize;
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * Check if file is an image
     */
    @Transient
    public boolean isImage() {
        return fileType != null && fileType.startsWith("image/");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Attachment)) return false;
        return id != null && id.equals(((Attachment) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Attachment{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", fileSize=" + getFormattedFileSize() +
                '}';
    }
}
