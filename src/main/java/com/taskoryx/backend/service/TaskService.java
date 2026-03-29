package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.request.task.CreateTaskRequest;
import com.taskoryx.backend.dto.request.task.MoveTaskRequest;
import com.taskoryx.backend.dto.request.task.TaskFilterRequest;
import com.taskoryx.backend.dto.request.task.UpdateTaskRequest;
import com.taskoryx.backend.dto.request.task.UpdateTaskStatusRequest;
import com.taskoryx.backend.dto.response.task.TaskResponse;
import com.taskoryx.backend.dto.response.task.TaskSummaryResponse;
import com.taskoryx.backend.entity.*;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.entity.IssueCategory;
import com.taskoryx.backend.entity.Version;
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
    private final VersionRepository versionRepository;
    private final IssueCategoryRepository issueCategoryRepository;
    private final TaskWatcherService taskWatcherService;

    @Transactional
    public TaskResponse createTask(UUID projectId, CreateTaskRequest request, UserPrincipal principal) {
        var project = projectService.findProjectWithAccess(projectId, principal.getId());

        Board board = null;
        BoardColumn column = null;

        if (request.getBoardId() != null) {
            board = boardRepository.findById(request.getBoardId())
                    .orElseThrow(() -> new ResourceNotFoundException("Board", "id", request.getBoardId()));
        }

        if (request.getColumnId() != null) {
            column = boardColumnRepository.findById(request.getColumnId())
                    .orElseThrow(() -> new ResourceNotFoundException("Column", "id", request.getColumnId()));
        }

        // Nếu có columnId thì phải có boardId
        if (column != null && board == null) {
            throw new BadRequestException("Phải cung cấp boardId khi chỉ định columnId");
        }

        User reporter = userRepository.findById(principal.getId()).orElseThrow();

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssigneeId()));
        }

        // Lấy task number tiếp theo
        int nextTaskNumber = taskRepository.findMaxTaskNumberByProjectId(projectId)
                .map(n -> n + 1).orElse(1);

        // Tính position
        BigDecimal position = column != null
                ? taskRepository.findMaxPositionByColumnId(column.getId())
                        .map(p -> p.add(BigDecimal.valueOf(1000))).orElse(BigDecimal.valueOf(1000))
                : BigDecimal.ZERO;

        Version version = null;
        if (request.getVersionId() != null) {
            version = versionRepository.findById(request.getVersionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Version", "id", request.getVersionId()));
        }

        IssueCategory category = null;
        if (request.getCategoryId() != null) {
            category = issueCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("IssueCategory", "id", request.getCategoryId()));
        }

        Task parentTask = null;
        if (request.getParentTaskId() != null) {
            parentTask = taskRepository.findById(request.getParentTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException("Task", "id", request.getParentTaskId()));
            if (!parentTask.getProject().getId().equals(projectId)) {
                throw new BadRequestException("Task cha phải thuộc cùng project");
            }
        }

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
                .version(version)
                .category(category)
                .parentTask(parentTask)
                .build();

        task = taskRepository.save(task);

        // Gán labels
        if (request.getLabelIds() != null && !request.getLabelIds().isEmpty()) {
            assignLabels(task, request.getLabelIds());
        }

        // Gửi notification nếu assign
        if (assignee != null && !assignee.getId().equals(principal.getId())) {
            notificationService.notifyTaskAssigned(
                    assignee.getId(), reporter.getFullName(),
                    task.getId(), task.getTitle(), project.getName());
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
    public TaskResponse getTaskByKey(String taskKey, UserPrincipal principal) {
        int dashIndex = taskKey.lastIndexOf('-');
        if (dashIndex < 0) {
            throw new ResourceNotFoundException("Task", "taskKey", taskKey);
        }
        String projectKey = taskKey.substring(0, dashIndex);
        int taskNumber;
        try {
            taskNumber = Integer.parseInt(taskKey.substring(dashIndex + 1));
        } catch (NumberFormatException e) {
            throw new ResourceNotFoundException("Task", "taskKey", taskKey);
        }
        Task task = taskRepository.findByProjectKeyAndTaskNumber(projectKey, taskNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "taskKey", taskKey));
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
        if (request.getStatus() != null) applyStatus(task, request.getStatus());
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
                notificationService.notifyTaskAssigned(
                        assignee.getId(), task.getReporter().getFullName(),
                        task.getId(), task.getTitle(), task.getProject().getName());
            }
        }

        if (request.getLabelIds() != null) {
            task.getTaskLabels().clear();
            assignLabels(task, request.getLabelIds());
        }

        // Handle version
        if (request.isClearVersion()) {
            task.setVersion(null);
        } else if (request.getVersionId() != null) {
            Version ver = versionRepository.findById(request.getVersionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Version", "id", request.getVersionId()));
            task.setVersion(ver);
        }

        // Handle category
        if (request.isClearCategory()) {
            task.setCategory(null);
        } else if (request.getCategoryId() != null) {
            IssueCategory cat = issueCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("IssueCategory", "id", request.getCategoryId()));
            task.setCategory(cat);
        }

        // Handle parent task
        if (request.isClearParent()) {
            task.setParentTask(null);
        } else if (request.getParentTaskId() != null) {
            if (request.getParentTaskId().equals(taskId)) {
                throw new BadRequestException("Task không thể là task cha của chính nó");
            }
            Task parent = taskRepository.findById(request.getParentTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException("Task", "id", request.getParentTaskId()));
            if (!parent.getProject().getId().equals(task.getProject().getId())) {
                throw new BadRequestException("Task cha phải thuộc cùng project");
            }
            // Tránh circular reference: parent không được là subtask của task hiện tại
            if (parent.getParentTask() != null && parent.getParentTask().getId().equals(taskId)) {
                throw new BadRequestException("Không thể tạo quan hệ cha-con vòng tròn");
            }
            task.setParentTask(parent);
        }

        Task saved = taskRepository.save(task);

        // Notify watchers about task update
        taskWatcherService.notifyWatchers(
                taskId, principal.getId(),
                "Task được cập nhật",
                "Task '" + saved.getTitle() + "' đã được cập nhật");

        return TaskResponse.from(saved);
    }

    @Transactional
    public TaskResponse moveTask(UUID taskId, MoveTaskRequest request, UserPrincipal principal) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        projectService.findProjectWithAccess(task.getProject().getId(), principal.getId());

        // targetColumnId = null → chuyển về Backlog
        if (request.getTargetColumnId() == null) {
            task.setColumn(null);
            task.setBoard(null);
            task.setPosition(request.getNewPosition());
            task.setCompletedAt(null);
            return TaskResponse.from(taskRepository.save(task));
        }

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

        // Nếu task đang ở backlog và chuyển vào board, cần set board
        if (task.getBoard() == null) {
            task.setBoard(targetColumn.getBoard());
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
    public List<TaskSummaryResponse> getBacklog(UUID projectId, UserPrincipal principal) {
        projectService.findProjectWithAccess(projectId, principal.getId());
        return taskRepository.findProductBacklog(projectId)
                .stream()
                .map(TaskSummaryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskSummaryResponse> getMyTasks(UserPrincipal principal) {
        PageRequest pageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.ASC, "dueDate"));
        return taskRepository.findByAssigneeId(principal.getId(), pageable)
                .map(TaskSummaryResponse::from)
                .getContent();
    }

    @Transactional
    public TaskResponse updateTaskStatus(UUID taskId, UpdateTaskStatusRequest request, UserPrincipal principal) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        projectService.findProjectWithAccess(task.getProject().getId(), principal.getId());
        applyStatus(task, request.getStatus());
        return TaskResponse.from(taskRepository.save(task));
    }

    /**
     * Áp dụng status và đồng bộ completedAt:
     * DONE / RESOLVED → set completedAt nếu chưa có
     * Trạng thái khác  → xóa completedAt
     */
    private void applyStatus(Task task, Task.TaskStatus status) {
        task.setStatus(status);
        boolean isDone = status == Task.TaskStatus.DONE || status == Task.TaskStatus.RESOLVED;
        if (isDone && task.getCompletedAt() == null) {
            task.setCompletedAt(LocalDateTime.now());
        } else if (!isDone) {
            task.setCompletedAt(null);
        }
    }

    private void assignLabels(Task task, List<UUID> labelIds) {
        // Labels are handled through TaskLabel entity - simplified here
        // Full implementation would save TaskLabel entities
    }
}
