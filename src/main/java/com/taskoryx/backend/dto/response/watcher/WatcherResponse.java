package com.taskoryx.backend.dto.response.watcher;

import com.taskoryx.backend.entity.TaskWatcher;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WatcherResponse {

    private UUID userId;
    private String username;
    private String fullName;
    private String avatarUrl;
    private LocalDateTime watchedAt;

    public static WatcherResponse from(TaskWatcher w) {
        return WatcherResponse.builder()
                .userId(w.getUser().getId())
                .username(w.getUser().getUsername())
                .fullName(w.getUser().getFullName())
                .avatarUrl(w.getUser().getAvatarUrl())
                .watchedAt(w.getCreatedAt())
                .build();
    }
}
