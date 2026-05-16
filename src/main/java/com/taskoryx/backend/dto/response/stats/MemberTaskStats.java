package com.taskoryx.backend.dto.response.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberTaskStats {

    private UUID userId;
    private String fullName;
    private String avatarUrl;

    private long total;
    private long inProgress;
    private long done;
    private long overdue;
}
