package com.taskoryx.backend.dto.response.gantt;

import com.taskoryx.backend.entity.Task;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GanttTaskItem {

    // ── Định danh ──────────────────────────────────────────────────────────────
    private UUID id;
    private String taskKey;
    private String title;
    private int depth;              // cấp task: 1, 2, 3
    private UUID parentTaskId;      // null nếu là task gốc
    private String parentTaskKey;

    // ── Trạng thái & phân loại ────────────────────────────────────────────────
    private Task.TaskStatus status;
    private String statusLabel;
    private Task.TaskPriority priority;
    private String priorityLabel;
    private String priorityColor;   // màu hex theo priority
    private int percentComplete;    // 0-100

    // ── Thời gian ─────────────────────────────────────────────────────────────
    private LocalDate startDate;
    private LocalDate dueDate;
    private boolean overdue;
    private Long daysOverdue;       // số ngày trễ, null nếu chưa quá hạn

    // ── Sprint ────────────────────────────────────────────────────────────────
    private UUID sprintId;
    private String sprintName;

    // ── Người thực hiện ───────────────────────────────────────────────────────
    private UUID assigneeId;
    private String assigneeName;
    private String assigneeAvatar;

    // ── Giờ làm ───────────────────────────────────────────────────────────────
    private BigDecimal estimatedHours;
    private BigDecimal actualHours;
    private Integer progressByHours; // phần trăm actual/estimated, null nếu không có estimated

    // ── Phân loại ─────────────────────────────────────────────────────────────
    private UUID categoryId;
    private String categoryName;

    // ── Phụ thuộc ─────────────────────────────────────────────────────────────
    private List<DependencyRef> dependencies;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DependencyRef {
        private UUID taskId;
        private String taskKey;
        private String type; // BLOCKS, DEPENDS_ON, RELATES_TO, DUPLICATES, PRECEDES, FOLLOWS
    }

    public static GanttTaskItem from(Task task) {
        LocalDate today = LocalDate.now();

        // percentComplete: ưu tiên dựa trên giờ thực tế/ước tính nếu có
        int percent = computePercent(task);

        // daysOverdue
        boolean overdue = task.getDueDate() != null
                && task.getDueDate().isBefore(today)
                && task.getCompletedAt() == null;
        Long daysOverdue = overdue
                ? today.toEpochDay() - task.getDueDate().toEpochDay()
                : null;

        // progressByHours
        Integer progressByHours = null;
        if (task.getEstimatedHours() != null
                && task.getEstimatedHours().compareTo(BigDecimal.ZERO) > 0
                && task.getActualHours() != null) {
            double ratio = task.getActualHours().doubleValue()
                    / task.getEstimatedHours().doubleValue() * 100;
            progressByHours = (int) Math.min(Math.round(ratio), 100);
        }

        List<DependencyRef> deps = task.getDependencies().stream()
                .map(d -> DependencyRef.builder()
                        .taskId(d.getDependsOnTask().getId())
                        .taskKey(d.getDependsOnTask().getTaskKey())
                        .type(d.getType() != null ? d.getType().name() : null)
                        .build())
                .collect(Collectors.toList());

        return GanttTaskItem.builder()
                .id(task.getId())
                .taskKey(task.getTaskKey())
                .title(task.getTitle())
                .depth(task.getDepth())
                .parentTaskId(task.getParentTask() != null ? task.getParentTask().getId() : null)
                .parentTaskKey(task.getParentTask() != null ? task.getParentTask().getTaskKey() : null)
                .status(task.getStatus())
                .statusLabel(statusLabel(task.getStatus()))
                .priority(task.getPriority())
                .priorityLabel(priorityLabel(task.getPriority()))
                .priorityColor(priorityColor(task.getPriority()))
                .percentComplete(percent)
                .startDate(task.getStartDate())
                .dueDate(task.getDueDate())
                .overdue(overdue)
                .daysOverdue(daysOverdue)
                .sprintId(task.getSprint() != null ? task.getSprint().getId() : null)
                .sprintName(task.getSprint() != null ? task.getSprint().getName() : null)
                .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                .assigneeName(task.getAssignee() != null ? task.getAssignee().getFullName() : null)
                .assigneeAvatar(task.getAssignee() != null ? task.getAssignee().getAvatarUrl() : null)
                .estimatedHours(task.getEstimatedHours())
                .actualHours(task.getActualHours())
                .progressByHours(progressByHours)
                .categoryId(task.getCategory() != null ? task.getCategory().getId() : null)
                .categoryName(task.getCategory() != null ? task.getCategory().getName() : null)
                .dependencies(deps)
                .build();
    }

    private static int computePercent(Task task) {
        // Nếu đã hoàn thành → 100%
        if (task.getStatus() == Task.TaskStatus.DONE
                || task.getStatus() == Task.TaskStatus.RESOLVED) return 100;
        if (task.getStatus() == Task.TaskStatus.CANCELLED) return 0;

        // Ưu tiên tính theo actual/estimated hours nếu có
        if (task.getEstimatedHours() != null
                && task.getEstimatedHours().compareTo(BigDecimal.ZERO) > 0
                && task.getActualHours() != null) {
            double ratio = task.getActualHours().doubleValue()
                    / task.getEstimatedHours().doubleValue() * 100;
            return (int) Math.min(Math.round(ratio), 99); // cap 99 vì chưa DONE
        }

        // Fallback theo status
        return switch (task.getStatus()) {
            case TODO        -> 0;
            case IN_PROGRESS -> 30;
            case IN_REVIEW   -> 70;
            default          -> 0;
        };
    }

    private static String statusLabel(Task.TaskStatus s) {
        return switch (s) {
            case TODO        -> "Cần làm";
            case IN_PROGRESS -> "Đang làm";
            case IN_REVIEW   -> "Đang review";
            case RESOLVED    -> "Đã giải quyết";
            case DONE        -> "Hoàn thành";
            case CANCELLED   -> "Đã hủy";
        };
    }

    private static String priorityLabel(Task.TaskPriority p) {
        return switch (p) {
            case LOW    -> "Thấp";
            case MEDIUM -> "Trung bình";
            case HIGH   -> "Cao";
            case URGENT -> "Khẩn cấp";
        };
    }

    private static String priorityColor(Task.TaskPriority p) {
        return switch (p) {
            case LOW    -> "#6b7280"; // gray
            case MEDIUM -> "#3b82f6"; // blue
            case HIGH   -> "#f97316"; // orange
            case URGENT -> "#ef4444"; // red
        };
    }
}
