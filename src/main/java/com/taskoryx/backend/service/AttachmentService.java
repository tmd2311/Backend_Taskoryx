package com.taskoryx.backend.service;

import com.taskoryx.backend.config.AppProperties;
import com.taskoryx.backend.dto.response.attachment.AttachmentResponse;
import com.taskoryx.backend.dto.response.attachment.AttachmentStatsResponse;
import com.taskoryx.backend.entity.Attachment;
import com.taskoryx.backend.entity.Comment;
import com.taskoryx.backend.entity.FileCategory;
import com.taskoryx.backend.entity.Task;
import com.taskoryx.backend.entity.User;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.exception.ForbiddenException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.AttachmentCategoryStatsProjection;
import com.taskoryx.backend.repository.AttachmentRepository;
import com.taskoryx.backend.repository.CommentRepository;
import com.taskoryx.backend.repository.TaskRepository;
import com.taskoryx.backend.repository.UserRepository;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service quản lý file đính kèm (Attachment).
 *
 * Design Notes:
 * - FileCategory là transient field, suy ra từ fileType (MIME) trong Java — không lưu DB.
 * - Lọc theo category thực hiện trong Java sau khi load từ DB (acceptable với số lượng file/task hạn chế).
 * - Stats dùng GROUP BY fileType query, aggregate về FileCategory trong Java.
 * - File streaming dùng Spring FileSystemResource, trả về ResponseEntity<Resource>.
 *
 * Dependencies:
 * - AttachmentRepository: truy vấn DB
 * - ProjectService: kiểm tra quyền truy cập project
 * - AppProperties: cấu hình uploadDir, maxFileSize, allowedTypes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final ProjectService projectService;
    private final AppProperties appProperties;

    /**
     * Lấy danh sách file đính kèm của một task, có thể lọc theo FileCategory.
     *
     * @param taskId    ID của task
     * @param category  Danh mục file cần lọc (null = tất cả)
     * @param principal Người dùng hiện tại
     * @return Danh sách AttachmentResponse
     */
    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachments(UUID taskId, FileCategory category, UserPrincipal principal) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        projectService.findProjectWithAccess(task.getProject().getId(), principal.getId());

        List<Attachment> attachments = attachmentRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
        return attachments.stream()
                .filter(a -> category == null || a.getFileCategory() == category)
                .map(AttachmentResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả file đính kèm trong một project, hỗ trợ phân trang và lọc theo category.
     *
     * Algorithm:
     * 1. Kiểm tra quyền truy cập project
     * 2. Load tất cả attachment của project (unpaged khi có filter)
     * 3. Lọc theo category nếu có
     * 4. Paginate thủ công với PageImpl
     *
     * @param projectId ID project
     * @param category  Danh mục lọc (null = tất cả)
     * @param pageable  Thông tin phân trang
     * @param principal Người dùng hiện tại
     * @return Trang AttachmentResponse
     */
    @Transactional(readOnly = true)
    public Page<AttachmentResponse> getProjectAttachments(UUID projectId, FileCategory category,
                                                           Pageable pageable, UserPrincipal principal) {
        projectService.findProjectWithAccess(projectId, principal.getId());

        if (category == null) {
            // Không có filter: dùng DB pagination
            Page<Attachment> page = attachmentRepository
                    .findByTask_Project_IdOrderByCreatedAtDesc(projectId, pageable);
            return page.map(AttachmentResponse::from);
        }

        // Có filter: load all, filter in Java, paginate manually
        Page<Attachment> all = attachmentRepository
                .findByTask_Project_IdOrderByCreatedAtDesc(projectId, Pageable.unpaged());
        List<AttachmentResponse> filtered = all.getContent().stream()
                .filter(a -> a.getFileCategory() == category)
                .map(AttachmentResponse::from)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<AttachmentResponse> pageContent = (start < filtered.size())
                ? filtered.subList(start, end) : List.of();
        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    /**
     * Thống kê số lượng file đính kèm theo FileCategory cho một task.
     * Luôn trả về đủ 9 category (0 nếu không có file).
     */
    @Transactional(readOnly = true)
    public AttachmentStatsResponse getTaskStats(UUID taskId, UserPrincipal principal) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        projectService.findProjectWithAccess(task.getProject().getId(), principal.getId());

        List<AttachmentCategoryStatsProjection> projections =
                attachmentRepository.getStatsByTaskId(taskId);
        return buildStatsResponse(projections);
    }

    /**
     * Thống kê số lượng file đính kèm theo FileCategory cho toàn bộ project.
     * Luôn trả về đủ 9 category (0 nếu không có file).
     */
    @Transactional(readOnly = true)
    public AttachmentStatsResponse getProjectStats(UUID projectId, UserPrincipal principal) {
        projectService.findProjectWithAccess(projectId, principal.getId());

        List<AttachmentCategoryStatsProjection> projections =
                attachmentRepository.getStatsByProjectId(projectId);
        return buildStatsResponse(projections);
    }

    /**
     * Phục vụ file đính kèm để download hoặc xem trực tiếp trong trình duyệt.
     *
     * @param attachmentId ID attachment
     * @param inline       true = Content-Disposition: inline (xem trực tiếp), false = attachment (download)
     * @param principal    Người dùng hiện tại
     * @return ResponseEntity chứa file Resource
     */
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> serveFile(UUID attachmentId, boolean inline, UserPrincipal principal) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "id", attachmentId));
        projectService.findProjectWithAccess(attachment.getTask().getProject().getId(), principal.getId());

        Path filePath = Paths.get(appProperties.getStorage().getUploadDir())
                .resolve(attachment.getStoragePath());
        Resource resource = new FileSystemResource(filePath);
        if (!resource.exists()) {
            throw new ResourceNotFoundException("File", "storage", attachmentId);
        }

        String disposition = inline ? "inline" : "attachment";
        String encodedName = UriUtils.encodePath(attachment.getFileName(), StandardCharsets.UTF_8);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(attachment.getFileType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        disposition + "; filename=\"" + encodedName + "\"")
                .body(resource);
    }

    @Transactional
    public AttachmentResponse uploadAttachment(UUID taskId, MultipartFile file,
                                                UUID commentId,
                                                UserPrincipal principal) throws IOException {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        projectService.findProjectWithAccess(task.getProject().getId(), principal.getId());

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

        // Resolve comment nếu có
        Comment comment = null;
        if (commentId != null) {
            comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));
            if (!comment.getTask().getId().equals(taskId)) {
                throw new BadRequestException("Comment không thuộc task này");
            }
        }

        String storagePath = saveFile(file, taskId);
        String fileUrl = "/api/attachments/files/" + storagePath;

        Attachment attachment = Attachment.builder()
                .task(task)
                .comment(comment)
                .uploadedBy(uploader)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .fileUrl(fileUrl)
                .storagePath(storagePath)
                .build();

        return AttachmentResponse.from(attachmentRepository.save(attachment));
    }

    /**
     * Lấy danh sách file đính kèm của một comment cụ thể.
     */
    @Transactional(readOnly = true)
    public List<AttachmentResponse> getCommentAttachments(UUID commentId, UserPrincipal principal) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));
        projectService.findProjectWithAccess(comment.getTask().getProject().getId(), principal.getId());
        return attachmentRepository.findByCommentIdOrderByCreatedAtAsc(commentId)
                .stream().map(AttachmentResponse::from).toList();
    }

    @Transactional
    public void deleteAttachment(UUID attachmentId, UserPrincipal principal) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "id", attachmentId));

        if (!attachment.getUploadedBy().getId().equals(principal.getId())) {
            throw new ForbiddenException("Bạn không có quyền xóa file này");
        }

        try {
            Path filePath = Paths.get(appProperties.getStorage().getUploadDir())
                    .resolve(attachment.getStoragePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", attachment.getStoragePath(), e);
        }

        attachmentRepository.delete(attachment);
    }

    /**
     * Aggregate danh sách projection (fileType → count) thành AttachmentStatsResponse.
     * Đảm bảo luôn có đủ 9 FileCategory, giá trị 0 nếu không có file.
     */
    private AttachmentStatsResponse buildStatsResponse(List<AttachmentCategoryStatsProjection> projections) {
        // Khởi tạo tất cả category với 0
        Map<FileCategory, Long> byCategory = new EnumMap<>(FileCategory.class);
        Arrays.stream(FileCategory.values()).forEach(cat -> byCategory.put(cat, 0L));

        // Tổng hợp từ projection: nhiều MIME type có thể map về cùng 1 FileCategory
        long totalCount = 0;
        for (AttachmentCategoryStatsProjection p : projections) {
            FileCategory cat = FileCategory.fromMimeType(p.getFileType());
            byCategory.merge(cat, p.getCount(), Long::sum);
            totalCount += p.getCount();
        }

        return AttachmentStatsResponse.builder()
                .totalCount(totalCount)
                .byCategory(byCategory)
                .build();
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
