package com.taskoryx.backend.dto.response.comment;

import com.taskoryx.backend.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private UUID id;
    private UUID taskId;
    private UUID userId;
    private String username;
    private String userFullName;
    private String userAvatar;
    private String content;
    private UUID parentId;
    private Boolean isEdited;
    private List<CommentResponse> replies;
    /**
     * Danh sách username đã mention (backward-compat).
     * VD: ["dung", "nam"]
     */
    private List<String> mentionedUsernames;
    /**
     * Thông tin đầy đủ của user được mention.
     * FE dùng để render mention chip, avatar, link profile.
     */
    private List<MentionedUserInfo> mentionedUsers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CommentResponse from(Comment comment) {
        List<MentionedUserInfo> mentionedUsers = comment.getMentions().stream()
                .map(m -> MentionedUserInfo.builder()
                        .userId(m.getUser().getId())
                        .username(m.getUser().getUsername())
                        .fullName(m.getUser().getFullName())
                        .avatarUrl(m.getUser().getAvatarUrl())
                        .build())
                .collect(Collectors.toList());

        return CommentResponse.builder()
                .id(comment.getId())
                .taskId(comment.getTask().getId())
                .userId(comment.getUser().getId())
                .username(comment.getUser().getUsername())
                .userFullName(comment.getUser().getFullName())
                .userAvatar(comment.getUser().getAvatarUrl())
                .content(comment.getContent())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .isEdited(comment.getIsEdited())
                .replies(comment.getReplies().stream()
                        .map(CommentResponse::from)
                        .collect(Collectors.toList()))
                .mentionedUsernames(mentionedUsers.stream()
                        .map(MentionedUserInfo::getUsername)
                        .collect(Collectors.toList()))
                .mentionedUsers(mentionedUsers)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
