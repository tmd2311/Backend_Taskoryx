package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.request.sprint.CreateSprintRequest;
import com.taskoryx.backend.dto.request.sprint.UpdateSprintRequest;
import com.taskoryx.backend.dto.response.board.KanbanBoardResponse;
import com.taskoryx.backend.dto.response.sprint.SprintResponse;
import com.taskoryx.backend.dto.response.task.TaskSummaryResponse;
import com.taskoryx.backend.entity.Board;
import com.taskoryx.backend.entity.BoardColumn;
import com.taskoryx.backend.entity.Project;
import com.taskoryx.backend.entity.ProjectPermission;
import com.taskoryx.backend.entity.Sprint;
import com.taskoryx.backend.entity.Task;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.BoardColumnRepository;
import com.taskoryx.backend.repository.BoardRepository;
import com.taskoryx.backend.repository.SprintRepository;
import com.taskoryx.backend.repository.TaskRepository;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SprintService {

    private final SprintRepository sprintRepository;
    private final TaskRepository taskRepository;
    private final BoardRepository boardRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final ProjectService projectService;
    private final BoardService boardService;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final ProjectCapabilityService projectCapabilityService;

    // ========== CREATE ==========

    public SprintResponse createSprint(UUID projectId, CreateSprintRequest request, UserPrincipal principal) {
        projectCapabilityService.requireModule(projectId, ProjectCapabilityService.MODULE_SPRINT, principal.getId());
        projectAuthorizationService.requirePermission(projectId, principal.getId(), ProjectPermission.SPRINT_MANAGE);
        Project project = projectService.findProjectWithAccess(projectId, principal.getId());

        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (!request.getStartDate().isBefore(request.getEndDate())) {
                throw new BadRequestException("Ngày bắt đầu phải trước ngày kết thúc");
            }
        }

        // Tự động tạo sprint board với các cột theo trạng thái task
        int maxPos = boardRepository.findMaxPositionByProjectId(projectId).orElse(-1);
        Board sprintBoard = Board.builder()
                .project(project)
                .name(request.getName())
                .boardType(Board.BoardType.SCRUM)
                .position(maxPos + 1)
                .isDefault(false)
                .build();
        sprintBoard = boardRepository.save(sprintBoard);
        createStatusColumns(sprintBoard);

        Sprint sprint = Sprint.builder()
                .project(project)
                .board(sprintBoard)
                .name(request.getName())
                .goal(request.getGoal())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(Sprint.SprintStatus.PLANNED)
                .build();

        sprint = sprintRepository.save(sprint);
        return SprintResponse.fromWithTasks(sprint, List.of());
    }

    /** Tạo 6 cột chuẩn theo TaskStatus cho sprint board */
    private void createStatusColumns(Board board) {
        record ColDef(String name, String color, Task.TaskStatus status, boolean completed) {}
        List<ColDef> defs = List.of(
            new ColDef("To Do",      "#6B7280", Task.TaskStatus.TODO,        false),
            new ColDef("In Progress","#3B82F6", Task.TaskStatus.IN_PROGRESS, false),
            new ColDef("In Review",  "#F59E0B", Task.TaskStatus.IN_REVIEW,   false),
            new ColDef("Resolved",   "#8B5CF6", Task.TaskStatus.RESOLVED,    false),
            new ColDef("Done",       "#10B981", Task.TaskStatus.DONE,        true),
            new ColDef("Cancelled",  "#EF4444", Task.TaskStatus.CANCELLED,   false)
        );
        for (int i = 0; i < defs.size(); i++) {
            ColDef d = defs.get(i);
            boardColumnRepository.save(BoardColumn.builder()
                    .board(board)
                    .name(d.name())
                    .color(d.color())
                    .mappedStatus(d.status())
                    .isCompleted(d.completed())
                    .position(i)
                    .build());
        }
    }

    // ========== READ ==========

    @Transactional(readOnly = true)
    public List<SprintResponse> getSprints(UUID projectId, UserPrincipal principal) {
        projectCapabilityService.requireModule(projectId, ProjectCapabilityService.MODULE_SPRINT, principal.getId());
        projectAuthorizationService.requirePermission(projectId, principal.getId(), ProjectPermission.BOARD_VIEW);
        return sprintRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(sprint -> SprintResponse.from(sprint, taskRepository.findBySprintIdOrderByCreatedAtAsc(sprint.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SprintResponse getSprint(UUID sprintId, UserPrincipal principal) {
        Sprint sprint = findSprintById(sprintId);
        projectCapabilityService.requireModule(sprint.getProject(), ProjectCapabilityService.MODULE_SPRINT);
        projectAuthorizationService.requirePermission(sprint.getProject().getId(), principal.getId(),
                ProjectPermission.BOARD_VIEW);
        return SprintResponse.fromWithTasks(sprint, taskRepository.findBySprintIdOrderByCreatedAtAsc(sprintId));
    }

    @Transactional(readOnly = true)
    public KanbanBoardResponse getSprintKanban(UUID sprintId, UserPrincipal principal) {
        Sprint sprint = findSprintById(sprintId);
        projectCapabilityService.requireModule(sprint.getProject(), ProjectCapabilityService.MODULE_SPRINT);
        projectAuthorizationService.requirePermission(sprint.getProject().getId(), principal.getId(),
                ProjectPermission.BOARD_VIEW);
        if (sprint.getBoard() == null) {
            throw new BadRequestException("Sprint chưa có kanban board");
        }
        return boardService.getKanbanBoard(sprint.getBoard().getId(), principal);
    }

    // ========== UPDATE ==========

    public SprintResponse updateSprint(UUID sprintId, UpdateSprintRequest request, UserPrincipal principal) {
        Sprint sprint = findSprintById(sprintId);
        projectCapabilityService.requireModule(sprint.getProject(), ProjectCapabilityService.MODULE_SPRINT);
        projectAuthorizationService.requirePermission(sprint.getProject().getId(), principal.getId(),
                ProjectPermission.SPRINT_MANAGE);

        if (sprint.getStatus() == Sprint.SprintStatus.COMPLETED
                || sprint.getStatus() == Sprint.SprintStatus.CANCELLED) {
            throw new BadRequestException("Không thể cập nhật sprint đã hoàn thành hoặc đã hủy");
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            sprint.setName(request.getName());
            if (sprint.getBoard() != null) {
                sprint.getBoard().setName(request.getName());
                boardRepository.save(sprint.getBoard());
            }
        }
        if (request.getGoal() != null) {
            sprint.setGoal(request.getGoal());
        }

        LocalDate newStart = request.getStartDate() != null ? request.getStartDate() : sprint.getStartDate();
        LocalDate newEnd   = request.getEndDate()   != null ? request.getEndDate()   : sprint.getEndDate();

        if (newStart != null && newEnd != null && !newStart.isBefore(newEnd)) {
            throw new BadRequestException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        if (request.getStartDate() != null) sprint.setStartDate(request.getStartDate());
        if (request.getEndDate()   != null) sprint.setEndDate(request.getEndDate());

        sprint = sprintRepository.save(sprint);
        return SprintResponse.fromWithTasks(sprint, taskRepository.findBySprintIdOrderByCreatedAtAsc(sprintId));
    }

    // ========== START SPRINT ==========

    public SprintResponse startSprint(UUID sprintId, UserPrincipal principal) {
        Sprint sprint = findSprintById(sprintId);
        projectCapabilityService.requireModule(sprint.getProject(), ProjectCapabilityService.MODULE_SPRINT);
        projectAuthorizationService.requirePermission(sprint.getProject().getId(), principal.getId(),
                ProjectPermission.SPRINT_MANAGE);

        if (sprint.getStatus() != Sprint.SprintStatus.PLANNED) {
            throw new BadRequestException("Chỉ có thể bắt đầu sprint đang ở trạng thái PLANNED");
        }

        if (sprintRepository.existsByProjectIdAndStatus(sprint.getProject().getId(), Sprint.SprintStatus.ACTIVE)) {
            throw new BadRequestException("Dự án đã có sprint đang hoạt động. Vui lòng hoàn thành sprint hiện tại trước");
        }

        sprint.setStatus(Sprint.SprintStatus.ACTIVE);
        if (sprint.getStartDate() == null) {
            sprint.setStartDate(LocalDate.now());
        }

        sprint = sprintRepository.save(sprint);
        return SprintResponse.fromWithTasks(sprint, taskRepository.findBySprintIdOrderByCreatedAtAsc(sprintId));
    }

    // ========== COMPLETE SPRINT ==========

    public SprintResponse completeSprint(UUID sprintId, UserPrincipal principal) {
        Sprint sprint = findSprintById(sprintId);
        projectCapabilityService.requireModule(sprint.getProject(), ProjectCapabilityService.MODULE_SPRINT);
        projectAuthorizationService.requirePermission(sprint.getProject().getId(), principal.getId(),
                ProjectPermission.SPRINT_MANAGE);

        if (sprint.getStatus() != Sprint.SprintStatus.ACTIVE) {
            throw new BadRequestException("Chỉ có thể hoàn thành sprint đang ở trạng thái ACTIVE");
        }

        sprint.setStatus(Sprint.SprintStatus.COMPLETED);
        sprint.setCompletedAt(LocalDateTime.now());

        sprint = sprintRepository.save(sprint);
        return SprintResponse.fromWithTasks(sprint, taskRepository.findBySprintIdOrderByCreatedAtAsc(sprintId));
    }

    // ========== DELETE ==========

    public void deleteSprint(UUID sprintId, UserPrincipal principal) {
        Sprint sprint = findSprintById(sprintId);
        projectCapabilityService.requireModule(sprint.getProject(), ProjectCapabilityService.MODULE_SPRINT);
        projectAuthorizationService.requirePermission(sprint.getProject().getId(), principal.getId(),
                ProjectPermission.SPRINT_MANAGE);

        if (sprint.getStatus() != Sprint.SprintStatus.PLANNED) {
            throw new BadRequestException("Chỉ có thể xóa sprint đang ở trạng thái PLANNED");
        }

        // Xóa sprint board đi kèm
        Board board = sprint.getBoard();
        clearTasksFromSprintBoard(sprint, board);
        sprint.setBoard(null);
        sprintRepository.save(sprint);

        if (board != null) {
            boardRepository.delete(board);
        }

        sprintRepository.delete(sprint);
    }

    // ========== TASK MANAGEMENT ==========

    public SprintResponse addTaskToSprint(UUID sprintId, UUID taskId, UserPrincipal principal) {
        Sprint sprint = findSprintById(sprintId);
        projectCapabilityService.requireModule(sprint.getProject(), ProjectCapabilityService.MODULE_SPRINT);
        projectAuthorizationService.requirePermission(sprint.getProject().getId(), principal.getId(),
                ProjectPermission.SPRINT_MANAGE);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        if (!task.getProject().getId().equals(sprint.getProject().getId())) {
            throw new BadRequestException("Task không thuộc dự án này");
        }
        ensureTaskNotInAnotherActiveOrPlannedSprint(sprint, task);

        task.setSprint(sprint);
        attachTaskToSprintBoard(sprint, task);
        taskRepository.save(task);
        Sprint refreshedSprint = findSprintById(sprintId);
        return SprintResponse.fromWithTasks(refreshedSprint, taskRepository.findBySprintIdOrderByCreatedAtAsc(sprintId));
    }

    public SprintResponse removeTaskFromSprint(UUID sprintId, UUID taskId, UserPrincipal principal) {
        Sprint sprint = findSprintById(sprintId);
        projectCapabilityService.requireModule(sprint.getProject(), ProjectCapabilityService.MODULE_SPRINT);
        projectAuthorizationService.requirePermission(sprint.getProject().getId(), principal.getId(),
                ProjectPermission.SPRINT_MANAGE);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        if (task.getSprint() == null || !task.getSprint().getId().equals(sprintId)) {
            throw new BadRequestException("Task không thuộc sprint này");
        }
        task.setSprint(null);
        detachTaskFromSprintBoard(sprint, task);
        taskRepository.save(task);
        Sprint refreshedSprint = findSprintById(sprintId);
        return SprintResponse.fromWithTasks(refreshedSprint, taskRepository.findBySprintIdOrderByCreatedAtAsc(sprintId));
    }

    // ========== HELPERS ==========

    private Sprint findSprintById(UUID sprintId) {
        return sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", "id", sprintId));
    }

    private void ensureTaskNotInAnotherActiveOrPlannedSprint(Sprint sprint, Task task) {
        if (task.getSprint() != null
                && !task.getSprint().getId().equals(sprint.getId())
                && (task.getSprint().getStatus() == Sprint.SprintStatus.PLANNED
                || task.getSprint().getStatus() == Sprint.SprintStatus.ACTIVE)) {
            throw new BadRequestException("Task đang thuộc một sprint khác đang PLANNED hoặc ACTIVE");
        }
    }

    private void attachTaskToSprintBoard(Sprint sprint, Task task) {
        if (sprint.getBoard() != null && task.getBoard() == null) {
            task.setBoard(sprint.getBoard());
        }
    }

    private void detachTaskFromSprintBoard(Sprint sprint, Task task) {
        // giữ nguyên board/column, chỉ bỏ liên kết sprint
    }

    private void clearTasksFromSprintBoard(Sprint sprint, Board board) {
        taskRepository.findBySprintIdOrderByCreatedAtAsc(sprint.getId()).forEach(task -> {
            task.setSprint(null);
            taskRepository.save(task);
        });
    }
}
