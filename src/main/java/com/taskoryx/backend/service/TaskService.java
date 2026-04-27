package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.request.task.CreateTaskRequest;
import com.taskoryx.backend.dto.request.task.MoveTaskRequest;
import com.taskoryx.backend.dto.request.task.TaskFilterRequest;
import com.taskoryx.backend.dto.request.task.UpdateTaskRequest;
import com.taskoryx.backend.dto.request.task.UpdateTaskStatusRequest;
import com.taskoryx.backend.dto.response.template.TemplateConfigDto;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final BoardRepository boardRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final LabelRepository labelRepository;
    private final ProjectService projectService;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final ProjectCapabilityService projectCapabilityService;
    private final NotificationService notificationService;
    private final IssueCategoryRepository issueCategoryRepository;
    private final SprintRepository sprintRepository;
    private final TaskWatcherService taskWatcherService;

    @Transactional
    public TaskResponse createTask(UUID projectId, CreateTaskRequest request, UserPrincipal principal) {
        projectAuthorizationService.requirePermission(projectId, principal.getId(), ProjectPermission.TASK_CREATE);
        var project = projectService.findProjectWithAccess(projectId, principal.getId());
        validateCreateRequestAgainstProjectConfig(project, request);

        Sprint sprint = findSprintInProject(projectId, request.getSprintId());
        if (sprint.getBoard() == null) {
            throw new BadRequestException("Sprint chưa có kanban board");
        }
        Board board = sprint.getBoard();
        Task.TaskStatus initialStatus = request.getStatus() != null ? request.getStatus() : Task.TaskStatus.TODO;
        BoardColumn column = boardColumnRepository.findByBoardIdAndMappedStatus(board.getId(), initialStatus)
                .orElseGet(() -> boardColumnRepository.findFirstByBoardIdOrderByPositionAsc(board.getId())
                        .orElseThrow(() -> new BadRequestException("Board của sprint chưa có cột nào")));

        User reporter = userRepository.findById(principal.getId()).orElseThrow();

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = findMemberUserInProject(projectId, request.getAssigneeId(), "assignee");
        }

        // Lấy task number tiếp theo
        int nextTaskNumber = taskRepository.findMaxTaskNumberByProjectId(projectId)
                .map(n -> n + 1).orElse(1);

        BigDecimal position = taskRepository.findMaxPositionByColumnId(column.getId())
                .map(p -> p.add(BigDecimal.valueOf(1000))).orElse(BigDecimal.valueOf(1000));

        IssueCategory category = null;
        if (request.getCategoryId() != null) {
            category = findCategoryInProject(projectId, request.getCategoryId());
        }

        Task parentTask = null;
        if (request.getParentTaskId() != null) {
            parentTask = taskRepository.findById(request.getParentTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException("Task", "id", request.getParentTaskId()));
            if (!parentTask.getProject().getId().equals(projectId)) {
                throw new BadRequestException("Task cha phải thuộc cùng project");
            }
            if (!parentTask.canHaveChildren()) {
                throw new BadRequestException("Task cha đã ở cấp 3, không thể thêm task con (giới hạn tối đa 3 cấp)");
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
                .status(initialStatus)
                .position(position)
                .assignee(assignee)
                .reporter(reporter)
                .startDate(request.getStartDate())
                .dueDate(request.getDueDate())
                .estimatedHours(request.getEstimatedHours())
                .category(category)
                .sprint(sprint)
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
        projectAuthorizationService.requirePermission(task.getProject().getId(), principal.getId(),
                ProjectPermission.TASK_VIEW);
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
        projectAuthorizationService.requirePermission(task.getProject().getId(), principal.getId(),
                ProjectPermission.TASK_VIEW);
        return TaskResponse.from(task);
    }

    @Transactional(readOnly = true)
    public Page<TaskSummaryResponse> getTasksByProject(UUID projectId, TaskFilterRequest filter,
                                                        UserPrincipal principal) {
        projectAuthorizationService.requirePermission(projectId, principal.getId(), ProjectPermission.TASK_VIEW);
        Sort sort = Sort.by(filter.getSortDir().equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC, filter.getSortBy());
        PageRequest pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);
        validateTaskFilter(projectId, filter);
        return taskRepository.findAll(buildTaskSpecification(projectId, filter), pageable)
                .map(TaskSummaryResponse::from);
    }

    @Transactional
    public TaskResponse updateTask(UUID taskId, UpdateTaskRequest request, UserPrincipal principal) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        projectAuthorizationService.requirePermission(task.getProject().getId(), principal.getId(),
                ProjectPermission.TASK_UPDATE);
        validateUpdateRequestAgainstProjectConfig(task, request);

        if (request.getTitle() != null) task.setTitle(request.getTitle());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        if (request.getStatus() != null) applyStatus(task, request.getStatus());
        if (request.getStartDate() != null) task.setStartDate(request.getStartDate());
        if (request.getDueDate() != null) task.setDueDate(request.getDueDate());
        if (request.getEstimatedHours() != null) task.setEstimatedHours(request.getEstimatedHours());
        if (request.getActualHours() != null) task.setActualHours(request.getActualHours());

        if (request.getAssigneeId() != null) {
            User assignee = findMemberUserInProject(task.getProject().getId(), request.getAssigneeId(), "assignee");
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

        // Handle category
        if (request.isClearCategory()) {
            task.setCategory(null);
        } else if (request.getCategoryId() != null) {
            IssueCategory cat = findCategoryInProject(task.getProject().getId(), request.getCategoryId());
            task.setCategory(cat);
        }

        // Handle sprint
        if (request.isClearSprint()) {
            detachTaskFromSprint(task);
        } else if (request.getSprintId() != null) {
            Sprint sprint = findSprintInProject(task.getProject().getId(), request.getSprintId());
            attachTaskToSprint(task, sprint);
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
            if (isDescendant(parent, taskId)) {
                throw new BadRequestException("Không thể tạo quan hệ cha-con vòng tròn");
            }
            if (!parent.canHaveChildren()) {
                throw new BadRequestException("Task cha đã ở cấp 3, không thể thêm task con (giới hạn tối đa 3 cấp)");
            }
            // Kiểm tra nếu gán parent mới, toàn bộ nhánh con vẫn không vượt quá cấp 3
            int parentDepth = parent.getDepth();
            int maxChildDepth = getMaxSubTreeDepth(task);
            if (parentDepth + maxChildDepth > 3) {
                throw new BadRequestException(
                        "Không thể gán task cha này vì sẽ khiến cây con vượt quá 3 cấp (cấp cha: "
                        + parentDepth + ", chiều sâu cây con: " + maxChildDepth + ")");
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
        projectAuthorizationService.requirePermission(task.getProject().getId(), principal.getId(),
                ProjectPermission.TASK_UPDATE);
        Sprint owningSprint = task.getSprint();

        BoardColumn targetColumn = boardColumnRepository.findById(request.getTargetColumnId())
                .orElseThrow(() -> new ResourceNotFoundException("Column", "id", request.getTargetColumnId()));
        if (!targetColumn.getBoard().getProject().getId().equals(task.getProject().getId())) {
            throw new BadRequestException("Không thể chuyển task sang cột của dự án khác");
        }
        Sprint targetSprint = sprintRepository.findByBoardId(targetColumn.getBoard().getId()).orElse(null);
        if (targetSprint != null && (owningSprint == null || !targetSprint.getId().equals(owningSprint.getId()))) {
            task.setSprint(targetSprint);
        } else if (targetSprint == null && owningSprint != null) {
            task.setSprint(null);
        }

        // Kiểm tra WIP limit
        if (targetColumn.getTaskLimit() != null) {
            long taskCount = taskRepository.countByProjectIdAndColumnId(
                    task.getProject().getId(), targetColumn.getId());
            if (taskCount >= targetColumn.getTaskLimit()) {
                throw new BadRequestException("Cột '" + targetColumn.getName() +
                        "' đã đạt giới hạn " + targetColumn.getTaskLimit() + " task");
            }
        }

        task.setBoard(targetColumn.getBoard());
        task.setColumn(targetColumn);
        task.setPosition(request.getNewPosition());

        // Nếu cột có mappedStatus → tự động set trạng thái task
        if (targetColumn.getMappedStatus() != null) {
            task.setStatus(targetColumn.getMappedStatus());
        }

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
        projectAuthorizationService.requirePermission(task.getProject().getId(), principal.getId(),
                ProjectPermission.TASK_DELETE);
        taskRepository.delete(task);
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
        projectAuthorizationService.requirePermission(task.getProject().getId(), principal.getId(),
                ProjectPermission.TASK_UPDATE);
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

    private void validateTaskFilter(UUID projectId, TaskFilterRequest filter) {
        if (filter.getColumnId() != null) {
            findColumnInProject(projectId, filter.getColumnId());
        }
        if (filter.getSprintId() != null) {
            findSprintInProject(projectId, filter.getSprintId());
        }
        if (filter.getAssigneeId() != null) {
            findMemberUserInProject(projectId, filter.getAssigneeId(), "assignee");
        }
        if (filter.getLabelIds() != null && !filter.getLabelIds().isEmpty()) {
            validateLabelIdsInProject(projectId, filter.getLabelIds());
        }
    }

    private Specification<Task> buildTaskSpecification(UUID projectId, TaskFilterRequest filter) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("project").get("id"), projectId));
            // Chỉ lấy task gốc (cấp 1); task con được nhúng vào subTasks của task cha
            predicates.add(cb.isNull(root.get("parentTask")));

            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                String keyword = "%" + filter.getKeyword().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), keyword),
                        cb.like(cb.lower(root.get("description")), keyword)
                ));
            }

            if (filter.getSprintId() != null) {
                predicates.add(cb.equal(root.get("sprint").get("id"), filter.getSprintId()));
            }

            if (filter.getColumnId() != null) {
                predicates.add(cb.equal(root.get("column").get("id"), filter.getColumnId()));
            }

            if (filter.getAssigneeId() != null) {
                predicates.add(cb.equal(root.get("assignee").get("id"), filter.getAssigneeId()));
            }

            if (filter.getPriorities() != null && !filter.getPriorities().isEmpty()) {
                predicates.add(root.get("priority").in(filter.getPriorities()));
            }

            if (filter.getLabelIds() != null && !filter.getLabelIds().isEmpty()) {
                var taskLabels = root.join("taskLabels");
                predicates.add(taskLabels.get("label").get("id").in(filter.getLabelIds()));
            }

            if (filter.getDueDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), filter.getDueDateFrom()));
            }

            if (filter.getDueDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), filter.getDueDateTo()));
            }

            if (Boolean.TRUE.equals(filter.getOverdue())) {
                predicates.add(cb.isNotNull(root.get("dueDate")));
                predicates.add(cb.lessThan(root.get("dueDate"), LocalDate.now()));
                predicates.add(cb.isNull(root.get("completedAt")));
            }

            if (filter.getCompleted() != null) {
                if (filter.getCompleted()) {
                    predicates.add(cb.isNotNull(root.get("completedAt")));
                } else {
                    predicates.add(cb.isNull(root.get("completedAt")));
                }
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private void validateCreateRequestAgainstProjectConfig(Project project, CreateTaskRequest request) {
        projectCapabilityService.requireModule(project, ProjectCapabilityService.MODULE_SPRINT);
        requireConfiguredTaskField(project, "dueDate", request.getDueDate(), "dueDate");
        requireConfiguredTaskField(project, "estimatedHours", request.getEstimatedHours(), "estimatedHours");
        requireConfiguredTaskField(project, "assignee", request.getAssigneeId(), "assignee");
        requireConfiguredTaskField(project, "labels", request.getLabelIds(), "labels");

        validateRequiredTaskFields(project,
                request.getPriority() != null ? request.getPriority() : Task.TaskPriority.MEDIUM,
                request.getDueDate(),
                request.getEstimatedHours(),
                request.getAssigneeId(),
                request.getLabelIds(),
                null);
    }

    private void validateUpdateRequestAgainstProjectConfig(Task task, UpdateTaskRequest request) {
        requireConfiguredTaskField(task.getProject(), "dueDate", request.getDueDate(), "dueDate");
        requireConfiguredTaskField(task.getProject(), "estimatedHours", request.getEstimatedHours(), "estimatedHours");
        requireConfiguredTaskField(task.getProject(), "assignee", request.getAssigneeId(), "assignee");
        requireConfiguredTaskField(task.getProject(), "labels", request.getLabelIds(), "labels");
        requireConfiguredTaskField(task.getProject(), "sprint", request.getSprintId(), "sprint");

        if (request.getSprintId() != null || request.isClearSprint()) {
            projectCapabilityService.requireModule(task.getProject(), ProjectCapabilityService.MODULE_SPRINT);
        }
        if (request.getActualHours() != null) {
            projectCapabilityService.requireModule(task.getProject(), ProjectCapabilityService.MODULE_TIME_TRACKING);
        }

        Task.TaskPriority effectivePriority = request.getPriority() != null ? request.getPriority() : task.getPriority();
        var effectiveDueDate = request.getDueDate() != null ? request.getDueDate() : task.getDueDate();
        var effectiveEstimatedHours = request.getEstimatedHours() != null ? request.getEstimatedHours() : task.getEstimatedHours();
        UUID effectiveAssigneeId = request.getAssigneeId() != null
                ? request.getAssigneeId()
                : task.getAssignee() != null ? task.getAssignee().getId() : null;
        List<UUID> effectiveLabelIds = request.getLabelIds() != null
                ? request.getLabelIds()
                : task.getTaskLabels().stream()
                        .map(tl -> tl.getLabel().getId())
                        .collect(Collectors.toList());
        UUID effectiveSprintId = request.isClearSprint()
                ? null
                : request.getSprintId() != null
                    ? request.getSprintId()
                    : task.getSprint() != null ? task.getSprint().getId() : null;

        if (request.isClearSprint() && hasTaskField(task.getProject(), "sprint")) {
            throw new BadRequestException("Không thể bỏ sprint vì đây là field bắt buộc của dự án");
        }

        validateRequiredTaskFields(task.getProject(),
                effectivePriority,
                effectiveDueDate,
                effectiveEstimatedHours,
                effectiveAssigneeId,
                effectiveLabelIds,
                effectiveSprintId);
    }

    private void validateRequiredTaskFields(Project project,
                                            Task.TaskPriority priority,
                                            Object dueDate,
                                            BigDecimal estimatedHours,
                                            UUID assigneeId,
                                            List<UUID> labelIds,
                                            UUID sprintId) {
        Set<String> fields = getConfiguredTaskFields(project);
        if (fields.isEmpty()) {
            return;
        }

        if (fields.contains("priority") && priority == null) {
            throw new BadRequestException("Dự án này yêu cầu field priority");
        }
        if (fields.contains("dueDate") && dueDate == null) {
            throw new BadRequestException("Dự án này yêu cầu field dueDate");
        }
        if (fields.contains("estimatedHours") && estimatedHours == null) {
            throw new BadRequestException("Dự án này yêu cầu field estimatedHours");
        }
        if (fields.contains("assignee") && assigneeId == null) {
            throw new BadRequestException("Dự án này yêu cầu field assignee");
        }
        if (fields.contains("labels") && (labelIds == null || labelIds.isEmpty())) {
            throw new BadRequestException("Dự án này yêu cầu field labels");
        }
        if (fields.contains("sprint") && sprintId == null) {
            throw new BadRequestException("Dự án này yêu cầu field sprint");
        }
    }

    private void requireConfiguredTaskField(Project project,
                                            String configuredField,
                                            Object value,
                                            String displayField) {
        if (!isProvided(value)) {
            return;
        }
        Set<String> fields = getConfiguredTaskFields(project);
        if (fields.isEmpty()) {
            return;
        }
        if (!fields.contains(normalizeField(configuredField))) {
            throw new BadRequestException("Dự án này không bật field " + displayField + " cho task");
        }
    }

    private boolean isProvided(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String stringValue) {
            return !stringValue.isBlank();
        }
        if (value instanceof List<?> listValue) {
            return !listValue.isEmpty();
        }
        return true;
    }

    private boolean hasTaskField(Project project, String fieldName) {
        return getConfiguredTaskFields(project).contains(normalizeField(fieldName));
    }

    private Set<String> getConfiguredTaskFields(Project project) {
        TemplateConfigDto config = projectCapabilityService.getProjectConfig(project);
        if (config == null || config.getTaskFields() == null) {
            return Set.of();
        }
        return config.getTaskFields().stream()
                .map(this::normalizeField)
                .collect(Collectors.toSet());
    }

    private String normalizeField(String fieldName) {
        return fieldName == null ? "" : fieldName.trim()
                .toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "");
    }

    private Board findBoardInProject(UUID projectId, UUID boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board", "id", boardId));
        if (!board.getProject().getId().equals(projectId)) {
            throw new BadRequestException("Board không thuộc dự án hiện tại");
        }
        return board;
    }

    private BoardColumn findColumnInProject(UUID projectId, UUID columnId) {
        BoardColumn column = boardColumnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("Column", "id", columnId));
        if (!column.getBoard().getProject().getId().equals(projectId)) {
            throw new BadRequestException("Cột không thuộc dự án hiện tại");
        }
        return column;
    }

    private IssueCategory findCategoryInProject(UUID projectId, UUID categoryId) {
        IssueCategory category = issueCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("IssueCategory", "id", categoryId));
        if (!category.getProject().getId().equals(projectId)) {
            throw new BadRequestException("Category không thuộc dự án hiện tại");
        }
        return category;
    }

    private Sprint findSprintInProject(UUID projectId, UUID sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", "id", sprintId));
        if (!sprint.getProject().getId().equals(projectId)) {
            throw new BadRequestException("Sprint không thuộc dự án hiện tại");
        }
        return sprint;
    }

    private void attachTaskToSprint(Task task, Sprint sprint) {
        if (task.getSprint() != null
                && !task.getSprint().getId().equals(sprint.getId())
                && (task.getSprint().getStatus() == Sprint.SprintStatus.PLANNED
                || task.getSprint().getStatus() == Sprint.SprintStatus.ACTIVE)) {
            throw new BadRequestException("Task đang thuộc một sprint khác đang PLANNED hoặc ACTIVE");
        }
        task.setSprint(sprint);
        if (task.getBoard() == null && sprint.getBoard() != null) {
            task.setBoard(sprint.getBoard());
        }
    }

    private void detachTaskFromSprint(Task task) {
        task.setSprint(null);
    }

    private void validateLabelIdsInProject(UUID projectId, List<UUID> labelIds) {
        for (UUID labelId : labelIds) {
            Label label = labelRepository.findById(labelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Label", "id", labelId));
            if (!label.getProject().getId().equals(projectId)) {
                throw new BadRequestException("Label không thuộc dự án hiện tại");
            }
        }
    }

    private User findMemberUserInProject(UUID projectId, UUID userId, String fieldName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new BadRequestException("Người dùng được chọn cho " + fieldName + " không thuộc dự án hiện tại");
        }
        return user;
    }

    /**
     * Lấy danh sách task hợp lệ để chọn làm cha.
     * Loại bỏ: task cấp 3 (không thể có con), chính task hiện tại, và toàn bộ cây con của nó.
     *
     * @param projectId project scope
     * @param excludeTaskId nếu khác null, loại task này và cây con của nó (dùng khi sửa task)
     */
    @Transactional(readOnly = true)
    public List<TaskSummaryResponse> getValidParentTasks(UUID projectId, UUID excludeTaskId,
                                                          UserPrincipal principal) {
        projectAuthorizationService.requirePermission(projectId, principal.getId(), ProjectPermission.TASK_VIEW);
        List<Task> candidates = taskRepository.findValidParentCandidates(projectId);

        if (excludeTaskId == null) {
            return candidates.stream()
                    .map(TaskSummaryResponse::from)
                    .collect(Collectors.toList());
        }

        // Lấy toàn bộ ID của task cần loại trừ + cây con
        Set<UUID> excludedIds = collectSubTreeIds(excludeTaskId);

        return candidates.stream()
                .filter(t -> !excludedIds.contains(t.getId()))
                .map(TaskSummaryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Kiểm tra xem {@code candidate} có phải là hậu duệ (con, cháu...) của task có id {@code ancestorId} không.
     */
    private boolean isDescendant(Task candidate, UUID ancestorId) {
        Task current = candidate.getParentTask();
        while (current != null) {
            if (current.getId().equals(ancestorId)) {
                return true;
            }
            current = current.getParentTask();
        }
        return false;
    }

    /**
     * Độ sâu tối đa của cây con (tính từ task làm gốc, không kể cha của nó).
     * Task lá → 1; task có con → 1 + max(con).
     */
    private int getMaxSubTreeDepth(Task task) {
        if (task.getSubTasks() == null || task.getSubTasks().isEmpty()) {
            return 1;
        }
        int max = 0;
        for (Task child : task.getSubTasks()) {
            int childDepth = getMaxSubTreeDepth(child);
            if (childDepth > max) max = childDepth;
        }
        return 1 + max;
    }

    /**
     * Thu thập tất cả UUID trong cây con (bao gồm chính task gốc).
     */
    private Set<UUID> collectSubTreeIds(UUID rootId) {
        Set<UUID> ids = new HashSet<>();
        collectSubTreeIdsRecursive(rootId, ids);
        return ids;
    }

    private void collectSubTreeIdsRecursive(UUID taskId, Set<UUID> ids) {
        ids.add(taskId);
        List<Task> children = taskRepository.findByParentTaskId(taskId);
        for (Task child : children) {
            collectSubTreeIdsRecursive(child.getId(), ids);
        }
    }
}
