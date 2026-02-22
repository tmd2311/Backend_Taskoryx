package com.taskoryx.backend.service;

import com.taskoryx.backend.config.AppProperties;
import com.taskoryx.backend.dto.response.attachment.AttachmentResponse;
import com.taskoryx.backend.entity.Attachment;
import com.taskoryx.backend.entity.Task;
import com.taskoryx.backend.entity.User;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.exception.ForbiddenException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.AttachmentRepository;
import com.taskoryx.backend.repository.TaskRepository;
import com.taskoryx.backend.repository.UserRepository;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final AppProperties appProperties;

    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachments(UUID taskId, UserPrincipal principal) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        projectService.findProjectWithAccess(task.getProject().getId(), principal.getId());

        return attachmentRepository.findByTaskIdOrderByCreatedAtDesc(taskId)
                .stream()
                .map(AttachmentResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public AttachmentResponse uploadAttachment(UUID taskId, MultipartFile file,
                                                UserPrincipal principal) throws IOException {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        projectService.findProjectWithAccess(task.getProject().getId(), principal.getId());

        // Validate file
        if (file.isEmpty()) {
            throw new BadRequestException("File không được để trống");
        }
        if (file.getSize() > appProperties.getStorage().getMaxFileSize()) {
            throw new BadRequestException("File quá lớn. Kích thước tối đa là 10MB");
        }
        if (!appProperties.getStorage().getAllowedTypes().contains(file.getContentType())) {
            throw new BadRequestException("Loại file không được hỗ trợ: " + file.getContentType());
        }

        User uploader = userRepository.findById(principal.getId()).orElseThrow();

        // Lưu file vào local storage
        String storagePath = saveFile(file, taskId);
        String fileUrl = "/api/attachments/files/" + storagePath;

        Attachment attachment = Attachment.builder()
                .task(task)
                .uploadedBy(uploader)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .fileUrl(fileUrl)
                .storagePath(storagePath)
                .build();

        return AttachmentResponse.from(attachmentRepository.save(attachment));
    }

    @Transactional
    public void deleteAttachment(UUID attachmentId, UserPrincipal principal) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "id", attachmentId));

        if (!attachment.getUploadedBy().getId().equals(principal.getId())) {
            throw new ForbiddenException("Bạn không có quyền xóa file này");
        }

        // Xóa file vật lý
        try {
            Path filePath = Paths.get(appProperties.getStorage().getUploadDir())
                    .resolve(attachment.getStoragePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", attachment.getStoragePath(), e);
        }

        attachmentRepository.delete(attachment);
    }

    private String saveFile(MultipartFile file, UUID taskId) throws IOException {
        String uploadDir = appProperties.getStorage().getUploadDir();
        String relativePath = "tasks/" + taskId + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path targetPath = Paths.get(uploadDir).resolve(relativePath);
        Files.createDirectories(targetPath.getParent());
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        return relativePath;
    }
}
