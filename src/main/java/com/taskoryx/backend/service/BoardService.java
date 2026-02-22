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
import com.taskoryx.backend.entity.Board;
import com.taskoryx.backend.entity.BoardColumn;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.BoardColumnRepository;
import com.taskoryx.backend.repository.BoardRepository;
import com.taskoryx.backend.repository.TaskRepository;
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
    private final ProjectService projectService;

    @Transactional(readOnly = true)
    public List<BoardResponse> getBoardsByProject(UUID projectId, UserPrincipal principal) {
        projectService.findProjectWithAccess(projectId, principal.getId());
        return boardRepository.findByProjectIdOrderByPositionAsc(projectId)
                .stream()
                .map(BoardResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public KanbanBoardResponse getKanbanBoard(UUID boardId, UserPrincipal principal) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board", "id", boardId));
        projectService.findProjectWithAccess(board.getProject().getId(), principal.getId());

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
                            .taskLimit(col.getTaskLimit())
                            .tasks(tasks)
                            .build();
                })
                .collect(Collectors.toList());

        return KanbanBoardResponse.builder()
                .boardId(board.getId())
                .boardName(board.getName())
                .projectId(board.getProject().getId())
                .projectName(board.getProject().getName())
                .columns(columnData)
                .build();
    }

    @Transactional
    public BoardResponse createBoard(UUID projectId, CreateBoardRequest request, UserPrincipal principal) {
        var project = projectService.findProjectWithAccess(projectId, principal.getId());
        int maxPos = boardRepository.findMaxPositionByProjectId(projectId).orElse(-1);
        Board board = Board.builder()
                .project(project)
                .name(request.getName())
                .description(request.getDescription())
                .position(maxPos + 1)
                .isDefault(false)
                .build();
        return BoardResponse.from(boardRepository.save(board));
    }

    @Transactional
    public BoardResponse updateBoard(UUID boardId, UpdateBoardRequest request, UserPrincipal principal) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board", "id", boardId));
        projectService.findProjectWithAccess(board.getProject().getId(), principal.getId());

        if (request.getName() != null) board.setName(request.getName());
        if (request.getDescription() != null) board.setDescription(request.getDescription());
        return BoardResponse.from(boardRepository.save(board));
    }

    @Transactional
    public void deleteBoard(UUID boardId, UserPrincipal principal) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board", "id", boardId));
        projectService.findProjectWithAccess(board.getProject().getId(), principal.getId());
        boardRepository.delete(board);
    }

    // ========== COLUMNS ==========

    @Transactional
    public BoardColumnResponse createColumn(UUID boardId, CreateColumnRequest request, UserPrincipal principal) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board", "id", boardId));
        projectService.findProjectWithAccess(board.getProject().getId(), principal.getId());

        Integer maxPos = boardColumnRepository.findMaxPositionByBoardId(boardId);
        BoardColumn column = BoardColumn.builder()
                .board(board)
                .name(request.getName())
                .color(request.getColor())
                .isCompleted(request.getIsCompleted() != null ? request.getIsCompleted() : false)
                .taskLimit(request.getTaskLimit())
                .position(maxPos != null ? maxPos + 1 : 0)
                .build();
        return BoardColumnResponse.from(boardColumnRepository.save(column));
    }

    @Transactional
    public BoardColumnResponse updateColumn(UUID columnId, UpdateColumnRequest request, UserPrincipal principal) {
        BoardColumn column = boardColumnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("Column", "id", columnId));
        projectService.findProjectWithAccess(column.getBoard().getProject().getId(), principal.getId());

        if (request.getName() != null) column.setName(request.getName());
        if (request.getColor() != null) column.setColor(request.getColor());
        if (request.getIsCompleted() != null) column.setIsCompleted(request.getIsCompleted());
        if (request.getTaskLimit() != null) column.setTaskLimit(request.getTaskLimit());
        return BoardColumnResponse.from(boardColumnRepository.save(column));
    }

    @Transactional
    public void moveColumn(UUID columnId, MoveColumnRequest request, UserPrincipal principal) {
        BoardColumn column = boardColumnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("Column", "id", columnId));
        projectService.findProjectWithAccess(column.getBoard().getProject().getId(), principal.getId());

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
        projectService.findProjectWithAccess(column.getBoard().getProject().getId(), principal.getId());
        boardColumnRepository.delete(column);
    }
}
