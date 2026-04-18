package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.request.comment.CreateCommentRequest;
import com.taskoryx.backend.dto.request.comment.UpdateCommentRequest;
import com.taskoryx.backend.dto.response.comment.CommentResponse;
import com.taskoryx.backend.entity.Comment;
import com.taskoryx.backend.entity.CommentMention;
import com.taskoryx.backend.entity.ProjectPermission;
import com.taskoryx.backend.entity.Task;
import com.taskoryx.backend.entity.User;
import com.taskoryx.backend.exception.ForbiddenException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.CommentMentionRepository;
import com.taskoryx.backend.repository.CommentRepository;
import com.taskoryx.backend.repository.TaskRepository;
import com.taskoryx.backend.repository.UserRepository;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentMentionRepository commentMentionRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ProjectAuthorizationService projectAuthorizationService;

    // Pattern để tìm @username trong comment
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9_-]+)");

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(UUID taskId, UserPrincipal principal) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        projectAuthorizationService.requirePermission(task.getProject().getId(), principal.getId(),
                ProjectPermission.TASK_VIEW);

        return commentRepository.findRootCommentsByTaskId(taskId)
                .stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentResponse createComment(UUID taskId, CreateCommentRequest request, UserPrincipal principal) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        projectAuthorizationService.requirePermission(task.getProject().getId(), principal.getId(),
                ProjectPermission.COMMENT_CREATE);

        User author = userRepository.findById(principal.getId()).orElseThrow();

        Comment comment = Comment.builder()
                .task(task)
                .user(author)
                .content(request.getContent())
                .isEdited(false)
                .build();

        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", request.getParentId()));
            comment.setParent(parent);
        }

        comment = commentRepository.save(comment);

        // Xử lý @mention
        processMentions(comment, task, author);

        // Notify task reporter/assignee about new comment
        notificationService.notifyTaskCommented(
                task.getId(), task.getTitle(),
                author.getFullName(), author.getId(),
                task.getAssignee() != null ? task.getAssignee().getId() : null,
                task.getReporter().getId());

        return CommentResponse.from(comment);
    }

    @Transactional
    public CommentResponse updateComment(UUID commentId, UpdateCommentRequest request, UserPrincipal principal) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));
        projectAuthorizationService.requirePermission(comment.getTask().getProject().getId(), principal.getId(),
                ProjectPermission.COMMENT_CREATE);

        if (!comment.getUser().getId().equals(principal.getId())) {
            throw new ForbiddenException("Bạn không có quyền chỉnh sửa bình luận này");
        }

        comment.setContent(request.getContent());
        comment.setIsEdited(true);

        // Xóa mention cũ, parse lại từ nội dung mới
        commentMentionRepository.deleteByCommentId(commentId);
        comment.getMentions().clear();
        comment = commentRepository.save(comment);
        processMentions(comment, comment.getTask(), comment.getUser());

        return CommentResponse.from(commentRepository.save(comment));
    }

    @Transactional
    public void deleteComment(UUID commentId, UserPrincipal principal) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        if (!comment.getUser().getId().equals(principal.getId())
                && !projectAuthorizationService.hasPermission(comment.getTask().getProject().getId(),
                principal.getId(), ProjectPermission.COMMENT_DELETE)) {
            throw new ForbiddenException("Bạn không có quyền xóa bình luận này");
        }
        commentRepository.delete(comment);
    }

    private void processMentions(Comment comment, Task task, User author) {
        Matcher matcher = MENTION_PATTERN.matcher(comment.getContent());
        // Dùng LinkedHashSet để giữ thứ tự + loại trùng lặp
        Set<String> uniqueUsernames = new LinkedHashSet<>();
        while (matcher.find()) {
            uniqueUsernames.add(matcher.group(1));
        }

        for (String username : uniqueUsernames) {
            userRepository.findByUsername(username).ifPresent(mentionedUser -> {
                CommentMention mention = CommentMention.builder()
                        .comment(comment)
                        .user(mentionedUser)
                        .build();
                comment.getMentions().add(mention);
                // Gửi notification (không notify chính mình)
                if (!mentionedUser.getId().equals(author.getId())) {
                    notificationService.notifyMention(
                            mentionedUser.getId(), author.getFullName(),
                            task.getTitle(), comment.getId());
                }
            });
        }
    }
}
