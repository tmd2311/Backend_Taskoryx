package com.taskoryx.backend.dto.response.board;

import com.taskoryx.backend.entity.Board;
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
public class BoardResponse {

    private UUID id;
    private UUID projectId;
    private String name;
    private String description;
    private Integer position;
    private Boolean isDefault;
    private List<BoardColumnResponse> columns;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BoardResponse from(Board board) {
        return BoardResponse.builder()
                .id(board.getId())
                .projectId(board.getProject().getId())
                .name(board.getName())
                .description(board.getDescription())
                .position(board.getPosition())
                .isDefault(board.getIsDefault())
                .columns(board.getColumns().stream()
                        .map(BoardColumnResponse::from)
                        .collect(Collectors.toList()))
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .build();
    }
}
