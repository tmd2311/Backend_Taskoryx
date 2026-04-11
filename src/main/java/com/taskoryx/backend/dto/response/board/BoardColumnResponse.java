package com.taskoryx.backend.dto.response.board;

import com.taskoryx.backend.entity.BoardColumn;
import com.taskoryx.backend.entity.Task;
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
public class BoardColumnResponse {

    private UUID id;
    private String name;
    private Integer position;
    private String color;
    private Boolean isCompleted;
    private Task.TaskStatus mappedStatus;
    private Integer taskLimit;
    private int taskCount;
    private LocalDateTime createdAt;

    public static BoardColumnResponse from(BoardColumn column) {
        return BoardColumnResponse.builder()
                .id(column.getId())
                .name(column.getName())
                .position(column.getPosition())
                .color(column.getColor())
                .isCompleted(column.getIsCompleted())
                .mappedStatus(column.getMappedStatus())
                .taskLimit(column.getTaskLimit())
                .taskCount(column.getTasks().size())
                .createdAt(column.getCreatedAt())
                .build();
    }
}
