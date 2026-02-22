package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.request.task.CreateTaskRequest;
import com.taskoryx.backend.dto.request.task.MoveTaskRequest;
import com.taskoryx.backend.dto.request.task.TaskFilterRequest;
import com.taskoryx.backend.dto.request.task.UpdateTaskRequest;
import com.taskoryx.backend.dto.response.task.TaskResponse;
import com.taskoryx.backend.dto.response.task.TaskSummaryResponse;
import com.taskoryx.backend.entity.*;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.*;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final BoardRepository boardRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final LabelRepository labelRepository;
    private final ProjectService projectService;
    private final NotificationService notificationService;

    @Transactional
    public TaskResponse createTask(UUID projectId, CreateTaskRequest request, UserPrincipal principal) {
        var project = projectService.findProjectWithAccess(projectId, principal.getId());

        Board board = boardRepository.findById(request.getBoardId())
                .orElseThrow(() -> new ResourceNotFoundException("Board", "id", request.getBoardId()));

        BoardColumn column = boardColumnRepository.findById(request.getColumnId())
                .orElseThrow(() -> new ResourceNotFoundException("Column", "id", request.getColumnId()));

        User reporter = userRepository.findById(principal.getId()).orElseThrow();

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssigneeId()));
        }

        // Lấy task number tiếp theo
        int nextTaskNumber = taskRepository.findMaxTaskNumberByProjectId(projectId)
                .map(n -> n + 1).orElse(1);

        // Tính position (cuối cột)
        BigDecimal position = taskRepository.findMaxPositionByColumnId(column.getId())
                .map(p -> p.add(BigDecimal.valueOf(1000))).orElse(BigDecimal.valueOf(1000));

        Task task = Task.builder()
                .project(project)
                .board(board)
                .column(column)
                .taskNumber(nextTaskNumber)
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : Task.TaskPriority.MEDIUM)
                .position(position)
                .assignee(assignee)
                .reporter(reporter)
                .startDate(request.getStartDate())
                .dueDate(request.getDueDate())
                .estimatedHours(request.getEstimatedHours())
                .build();

        task = taskRepository.save(task);

        // Gán labels
        if (request.getLabelIds() != null && !request.getLabelIds().isEmpty()) {
            assignLabels(task, request.getLabelIds());
        }

        // Gửi notification nếu assign
        if (assignee != null && !assignee.getId().equals(principal.getId())) {
            notificationService.notifyTaskAssigned(task, assignee, reporter);
        }

        return TaskResponse.from(taskRepository.findById(task.getId()).orElseThrow());
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(UUID taskId, UserPrincipal principal) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        projectService.findProjectWithAccess(task.getProject().getId(), principal.getId());
        return TaskResponse.from(task);
    }

    @Transactional(readOnly = true)
    public Page<TaskSummaryResponse> getTasksByProject(UUID projectId, TaskFilterRequest filter,
                                                        UserPrincipal principal) {
        projectService.findProjectWithAccess(projectId, principal.getId());
        Sort sort = Sort.by(filter.getSortDir().equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC, filter.getSortBy());
        PageRequest pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
            return taskRepository.searchByProjectId(projectId, filter.getKeyword(), pageable)
                    .map(TaskSummaryResponse::from);
        }
        return taskRepository.findByProjectId(projectId, pageable).map(TaskSummaryResponse::from);
    }

    @Transactional
    public TaskResponse updateTask(UUID taskId, UpdateTaskRequest request, UserPrincipal principal) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        projectService.findProjectWithAccess(task.getProject().getId(), principal.getId());

        if (request.getTitle() != null) task.setTitle(request.getTitle());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        if (request.getStartDate() != null) task.setStartDate(request.getStartDate());
        if (request.getDueDate() != null) task.setDueDate(request.getDueDate());
        if (request.getEstimatedHours() != null) task.setEstimatedHours(request.getEstimatedHours());
        if (request.getActualHours() != null) task.setActualHours(request.getActualHours());

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssigneeId()));
            boolean newAssignee = task.getAssignee() == null ||
                    !task.getAssignee().getId().equals(request.getAssigneeId());
            task.setAssignee(assignee);
            if (newAssignee && !assignee.getId().equals(principal.getId())) {
                notificationService.notifyTaskAssigned(task, assignee, task.getReporter());
            }
        }

        if (request.getLabelIds() != null) {
            task.getTaskLabels().clear();
            assignLabels(task, request.getLabelIds());
        }

        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse moveTask(UUID taskId, MoveTaskRequest request, UserPrincipal principal) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        projectService.findProjectWithAccess(task.getProject().getId(), principal.getId());

        BoardColumn targetColumn = boardColumnRepository.findById(request.getTargetColumnId())
                .orElseThrow(() -> new ResourceNotFoundException("Column", "id", request.getTargetColumnId()));

        // Kiểm tra WIP limit
        if (targetColumn.getTaskLimit() != null) {
            long taskCount = taskRepository.countByProjectIdAndColumnId(
                    task.getProject().getId(), targetColumn.getId());
            if (taskCount >= targetColumn.getTaskLimit()) {
                throw new BadRequestException("Cột '" + targetColumn.getName() +
                        "' đã đạt giới hạn " + targetColumn.getTaskLimit() + " task");
            }
        }

        task.setColumn(targetColumn);
        task.setPosition(request.getNewPosition());

        // Đánh dấu hoàn thành nếu chuyển vào cột đã hoàn thành
        if (targetColumn.getIsCompleted() && task.getCompletedAt() == null) {
            task.setCompletedAt(LocalDateTime.now());
        } else if (!targetColumn.getIsCompleted() && task.getCompletedAt() != null) {
            task.setCompletedAt(null);
        }

        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(UUID taskId, UserPrincipal principal) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        projectService.findProjectWithAccess(task.getProject().getId(), principal.getId());
        taskRepository.delete(task);
    }

    @Transactional(readOnly = true)
    public List<TaskSummaryResponse> getMyTasks(UserPrincipal principal) {
        PageRequest pageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.ASC, "dueDate"));
        return taskRepository.findByAssigneeId(principal.getId(), pageable)
                .map(TaskSummaryResponse::from)
                .getContent();
    }

    private void assignLabels(Task task, List<UUID> labelIds) {
        // Labels are handled through TaskLabel entity - simplified here
        // Full implementation would save TaskLabel entities
    }
}
