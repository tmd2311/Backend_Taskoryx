package com.taskoryx.backend.dto.response.board;

import com.taskoryx.backend.dto.response.task.TaskSummaryResponse;
import com.taskoryx.backend.entity.Board;
import com.taskoryx.backend.entity.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Response cho Kanban board view - dùng cho drag & drop
 * Chứa toàn bộ columns và tasks được nhóm theo column
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KanbanBoardResponse {

    private UUID boardId;
    private String boardName;
    private UUID projectId;
    private String projectName;
    private Board.BoardType boardType;
    private Boolean isSprintBoard;
    private UUID sprintId;
    private String sprintName;
    private UUID ownerId;
    private List<KanbanColumnData> columns;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KanbanColumnData {
        private UUID id;
        private String name;
        private Integer position;
        private String color;
        private Boolean isCompleted;
        private Task.TaskStatus mappedStatus;
        private Integer taskLimit;
        private List<TaskSummaryResponse> tasks;
    }
}
