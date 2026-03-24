package com.taskoryx.backend.dto.response.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Thông tin rút gọn của user được mention trong comment.
 * FE dùng để render mention chip / highlight @username.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentionedUserInfo {

    private UUID userId;
    private String username;
    private String fullName;
    private String avatarUrl;
}
