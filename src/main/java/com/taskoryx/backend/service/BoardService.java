package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.request.board.CreateBoardRequest;
import com.taskoryx.backend.dto.request.board.CreateColumnRequest;
import com.taskoryx.backend.dto.request.board.MoveColumnRequest;
import com.taskoryx.backend.dto.request.board.UpdateBoardRequest;
import com.taskoryx.backend.dto.request.board.UpdateColumnRequest;
import com.taskoryx.backend.dto.response.board.BoardColumnResponse;
import com.taskoryx.backend.dto.response.board.BoardResponse;
import com.taskoryx.backend.dto.response.board.KanbanBoardResponse;
import com.taskoryx.backend.dto.response.task.TaskSummaryResponse;
import com.taskoryx.backend.entity.ActivityLog;
import com.taskoryx.backend.entity.Board;
import com.taskoryx.backend.entity.BoardColumn;
import com.taskoryx.backend.entity.Task;
import com.taskoryx.backend.entity.ProjectPermission;
import com.taskoryx.backend.entity.Sprint;
import com.taskoryx.backend.entity.User;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.BoardColumnRepository;
import com.taskoryx.backend.repository.BoardRepository;
import com.taskoryx.backend.repository.SprintRepository;
import com.taskoryx.backend.repository.TaskRepository;
import com.taskoryx.backend.repository.UserRepository;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final SprintRepository sprintRepository;
    private final ProjectService projectService;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<BoardResponse> getBoardsByProject(UUID projectId, UserPrincipal principal) {
        projectAuthorizationService.requirePermission(projectId, principal.getId(), ProjectPermission.BOARD_VIEW);
        return boardRepository.findByProjectIdOrderByPositionAsc(projectId)
                .stream()
                .map(this::toBoardResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public KanbanBoardResponse getKanbanBoard(UUID boardId, UserPrincipal principal) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board", "id", boardId));
        projectAuthorizationService.requirePermission(board.getProject().getId(), principal.getId(),
                ProjectPermission.BOARD_VIEW);

        List<BoardColumn> columns = boardColumnRepository.findByBoardIdOrderByPositionAsc(boardId);

        List<KanbanBoardResponse.KanbanColumnData> columnData = columns.stream()
                .map(col -> {
                    List<TaskSummaryResponse> tasks = taskRepository
                            .findByColumnIdOrderByPositionAsc(col.getId())
                            .stream()
                            .map(TaskSummaryResponse::from)
                            .collect(Collectors.toList());
                    return KanbanBoardResponse.KanbanColumnData.builder()
                            .id(col.getId())
                            .name(col.getName())
                            .position(col.getPosition())
                            .color(col.getColor())
                            .isCompleted(col.getIsCompleted())
                            .mappedStatus(col.getMappedStatus())
                            .taskLimit(col.getTaskLimit())
                            .tasks(tasks)
                            .build();
                })
                .collect(Collectors.toList());

        Sprint sprint = sprintRepository.findByBoardId(boardId).orElse(null);

        return KanbanBoardResponse.builder()
                .boardId(board.getId())
                .boardName(board.getName())
                .projectId(board.getProject().getId())
                .projectName(board.getProject().getName())
                .boardType(board.getBoardType())
                .isSprintBoard(sprint != null)
                .sprintId(sprint != null ? sprint.getId() : null)
                .sprintName(sprint != null ? sprint.getName() : null)
                .ownerId(board.getOwner() != null ? board.getOwner().getId() : null)
                .columns(columnData)
                .build();
    }

    @Transactional
    public BoardResponse createBoard(UUID projectId, CreateBoardRequest request, UserPrincipal principal) {
        projectAuthorizationService.requirePermission(projectId, principal.getId(), ProjectPermission.BOARD_UPDATE);
        var project = projectService.findProjectWithAccess(projectId, principal.getId());

        Board.BoardType boardType = request.getBoardType() != null ? request.getBoardType() : Board.BoardType.KANBAN;

        int maxPos = boardRepository.findMaxPositionByProjectId(projectId).orElse(-1);

        User owner = null;
        if (boardType == Board.BoardType.PERSONAL) {
            owner = userRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));
        }

        Board board = Board.builder()
                .project(project)
                .name(request.getName())
                .description(request.getDescription())
                .position(maxPos + 1)
                .boardType(boardType)
                .owner(owner)
                .isDefault(false)
                .build();
        board = boardRepository.save(board);

        if (boardType == Board.BoardType.PERSONAL) {
            BoardColumn doneColumn = BoardColumn.builder()
                    .board(board)
                    .name("Hoàn thành")
                    .position(0)
                    .color("#22c55e")
                    .isCompleted(true)
                    .mappedStatus(Task.TaskStatus.DONE)
                    .build();
            boardColumnRepository.save(doneColumn);
        }

        User actor = userRepository.findById(principal.getId()).orElseThrow();
        String createDesc = actor.getFullName() + " đã tạo board \"" + board.getName() + "\" trong dự án \"" + project.getName() + "\"";
        activityLogService.logActivity(actor, project,
                ActivityLog.EntityType.BOARD, board.getId(), ActivityLog.Action.CREATE,
                board.getName(), createDesc,
                null, "{\"boardName\":\"" + board.getName() + "\",\"boardType\":\"" + boardType.name() + "\"}");

        return toBoardResponse(board);
    }

    @Transactional
    public BoardResponse updateBoard(UUID boardId, UpdateBoardRequest request, UserPrincipal principal) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board", "id", boardId));
        projectAuthorizationService.requirePermission(board.getProject().getId(), principal.getId(),
                ProjectPermission.BOARD_UPDATE);

        if (request.getName() != null) board.setName(request.getName());
        if (request.getDescription() != null) board.setDescription(request.getDescription());
        Board saved = boardRepository.save(board);

        User actor = userRepository.findById(principal.getId()).orElseThrow();
        String updateDesc = actor.getFullName() + " đã cập nhật board \"" + saved.getName() + "\"";
        activityLogService.logActivity(actor, saved.getProject(),
                ActivityLog.EntityType.BOARD, saved.getId(), ActivityLog.Action.UPDATE,
                saved.getName(), updateDesc,
                null, "{\"boardName\":\"" + saved.getName() + "\"}");

        return toBoardResponse(saved);
    }

    @Transactional
    public void deleteBoard(UUID boardId, UserPrincipal principal) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board", "id", boardId));
        projectAuthorizationService.requirePermission(board.getProject().getId(), principal.getId(),
                ProjectPermission.BOARD_UPDATE);
        if (isSprintManagedBoard(board)) {
            throw new BadRequestException("Không thể xóa board sprint thủ công — board này được quản lí tự động theo sprint");
        }

        User actor = userRepository.findById(principal.getId()).orElseThrow();
        String deleteDesc = actor.getFullName() + " đã xóa board \"" + board.getName() + "\"";
        activityLogService.logActivity(actor, board.getProject(),
                ActivityLog.EntityType.BOARD, board.getId(), ActivityLog.Action.DELETE,
                board.getName(), deleteDesc,
                "{\"boardName\":\"" + board.getName() + "\"}", null);

        boardRepository.delete(board);
    }

    // ========== COLUMNS ==========

    @Transactional
    public BoardColumnResponse createColumn(UUID boardId, CreateColumnRequest request, UserPrincipal principal) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board", "id", boardId));
        projectAuthorizationService.requirePermission(board.getProject().getId(), principal.getId(),
                ProjectPermission.BOARD_UPDATE);
        if (isSprintManagedBoard(board)) {
            throw new BadRequestException("Không thể thêm cột vào board sprint — các cột được quản lí tự động theo trạng thái task");
        }
        if (board.getBoardType() == Board.BoardType.PERSONAL && request.getMappedStatus() == null) {
            throw new BadRequestException("Board cá nhân yêu cầu phải chọn trạng thái (mappedStatus) cho mỗi cột");
        }

        Integer maxPos = boardColumnRepository.findMaxPositionByBoardId(boardId);
        BoardColumn column = BoardColumn.builder()
                .board(board)
                .name(request.getName())
                .color(request.getColor())
                .isCompleted(request.getIsCompleted() != null ? request.getIsCompleted() : false)
                .taskLimit(request.getTaskLimit())
                .mappedStatus(request.getMappedStatus())
                .position(maxPos != null ? maxPos + 1 : 0)
                .build();
        BoardColumn savedColumn = boardColumnRepository.save(column);

        User actor = userRepository.findById(principal.getId()).orElseThrow();
        String createColDesc = actor.getFullName() + " đã thêm cột \"" + savedColumn.getName() + "\" vào board \"" + board.getName() + "\"";
        activityLogService.logActivity(actor, board.getProject(),
                ActivityLog.EntityType.COLUMN, savedColumn.getId(), ActivityLog.Action.CREATE,
                savedColumn.getName(), createColDesc,
                null, "{\"columnName\":\"" + savedColumn.getName() + "\",\"boardName\":\"" + board.getName() + "\"}");

        return BoardColumnResponse.from(savedColumn);
    }

    @Transactional
    public BoardColumnResponse updateColumn(UUID columnId, UpdateColumnRequest request, UserPrincipal principal) {
        BoardColumn column = boardColumnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("Column", "id", columnId));
        projectAuthorizationService.requirePermission(column.getBoard().getProject().getId(), principal.getId(),
                ProjectPermission.BOARD_UPDATE);
        if (isSprintManagedBoard(column.getBoard())) {
            throw new BadRequestException("Không thể chỉnh sửa cột của board sprint");
        }

        if (request.getName() != null) column.setName(request.getName());
        if (request.getColor() != null) column.setColor(request.getColor());
        if (request.getIsCompleted() != null) column.setIsCompleted(request.getIsCompleted());
        if (request.getTaskLimit() != null) column.setTaskLimit(request.getTaskLimit());
        if (request.getMappedStatus() != null) column.setMappedStatus(request.getMappedStatus());
        BoardColumn savedCol = boardColumnRepository.save(column);

        User actor = userRepository.findById(principal.getId()).orElseThrow();
        String updateColDesc = actor.getFullName() + " đã cập nhật cột \"" + savedCol.getName() + "\" trên board \"" + savedCol.getBoard().getName() + "\"";
        activityLogService.logActivity(actor, savedCol.getBoard().getProject(),
                ActivityLog.EntityType.COLUMN, savedCol.getId(), ActivityLog.Action.UPDATE,
                savedCol.getName(), updateColDesc,
                null, "{\"columnName\":\"" + savedCol.getName() + "\",\"boardName\":\"" + savedCol.getBoard().getName() + "\"}");

        return BoardColumnResponse.from(savedCol);
    }

    @Transactional
    public void moveColumn(UUID columnId, MoveColumnRequest request, UserPrincipal principal) {
        BoardColumn column = boardColumnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("Column", "id", columnId));
        projectAuthorizationService.requirePermission(column.getBoard().getProject().getId(), principal.getId(),
                ProjectPermission.BOARD_UPDATE);
        if (isSprintManagedBoard(column.getBoard())) {
            throw new BadRequestException("Không thể di chuyển cột của board sprint");
        }

        List<BoardColumn> columns = boardColumnRepository
                .findByBoardIdOrderByPositionAsc(column.getBoard().getId());

        // Reorder columns
        columns.remove(column);
        int newPos = Math.min(request.getNewPosition(), columns.size());
        columns.add(newPos, column);

        for (int i = 0; i < columns.size(); i++) {
            columns.get(i).setPosition(i);
        }
        boardColumnRepository.saveAll(columns);
    }

    @Transactional
    public void deleteColumn(UUID columnId, UserPrincipal principal) {
        BoardColumn column = boardColumnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("Column", "id", columnId));
        projectAuthorizationService.requirePermission(column.getBoard().getProject().getId(), principal.getId(),
                ProjectPermission.BOARD_UPDATE);
        if (isSprintManagedBoard(column.getBoard())) {
            throw new BadRequestException("Không thể xóa cột của board sprint");
        }

        User actor = userRepository.findById(principal.getId()).orElseThrow();
        String deleteColDesc = actor.getFullName() + " đã xóa cột \"" + column.getName() + "\" khỏi board \"" + column.getBoard().getName() + "\"";
        activityLogService.logActivity(actor, column.getBoard().getProject(),
                ActivityLog.EntityType.COLUMN, column.getId(), ActivityLog.Action.DELETE,
                column.getName(), deleteColDesc,
                "{\"columnName\":\"" + column.getName() + "\",\"boardName\":\"" + column.getBoard().getName() + "\"}", null);

        boardColumnRepository.delete(column);
    }

    private BoardResponse toBoardResponse(Board board) {
        BoardResponse response = BoardResponse.from(board);
        sprintRepository.findByBoardId(board.getId()).ifPresent(sprint -> {
            response.setIsSprintBoard(true);
            response.setSprintId(sprint.getId());
            response.setSprintName(sprint.getName());
        });
        return response;
    }

    private boolean isSprintManagedBoard(Board board) {
        return sprintRepository.findByBoardId(board.getId()).isPresent();
    }
}
