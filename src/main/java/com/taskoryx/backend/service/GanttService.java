package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.response.gantt.GanttResponse;
import com.taskoryx.backend.dto.response.gantt.GanttTaskItem;
import com.taskoryx.backend.entity.ProjectPermission;
import com.taskoryx.backend.entity.Task;
import com.taskoryx.backend.repository.TaskRepository;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GanttService {

    private final TaskRepository taskRepository;
    private final ProjectAuthorizationService projectAuthorizationService;

    @Transactional(readOnly = true)
    public GanttResponse getGanttData(UUID projectId, UUID sprintId, UUID assigneeId,
                                      LocalDate dateFrom, LocalDate dateTo,
                                      UserPrincipal principal) {
        projectAuthorizationService.requirePermission(projectId, principal.getId(), ProjectPermission.TASK_VIEW);

        // Chọn query phù hợp: nếu filter theo sprint thì dùng query riêng
        List<Task> tasks = (sprintId != null)
                ? taskRepository.findForGanttBySprint(projectId, sprintId)
                : taskRepository.findForGantt(projectId);

        // Áp dụng filter in-memory
        if (assigneeId != null) {
            tasks = tasks.stream()
                    .filter(t -> t.getAssignee() != null && assigneeId.equals(t.getAssignee().getId()))
                    .collect(Collectors.toList());
        }
        if (dateFrom != null || dateTo != null) {
            tasks = tasks.stream()
                    .filter(t -> {
                        LocalDate ref = effectiveDate(t);
                        if (ref == null) return true; // task không có ngày → vẫn hiện
                        if (dateFrom != null && ref.isBefore(dateFrom)) return false;
                        if (dateTo   != null && ref.isAfter(dateTo))    return false;
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        // Sắp xếp: task cha trước con, theo taskNumber trong cùng cấp
        List<Task> sorted = tasks.stream()
                .sorted(Comparator
                        .comparing(t -> t.getParentTask() == null ? 0 : 1)  // cha lên trước
                )
                .sorted(Comparator.comparingInt(Task::getTaskNumber))
                .collect(Collectors.toList());

        List<GanttTaskItem> items = sorted.stream()
                .map(GanttTaskItem::from)
                .collect(Collectors.toList());

        // Tính khoảng thời gian bao phủ
        LocalDate rangeStart = tasks.stream()
                .filter(t -> t.getStartDate() != null || t.getDueDate() != null)
                .map(t -> t.getStartDate() != null ? t.getStartDate() : t.getDueDate())
                .min(Comparator.naturalOrder())
                .orElse(LocalDate.now());

        LocalDate rangeEnd = tasks.stream()
                .filter(t -> t.getDueDate() != null || t.getStartDate() != null)
                .map(t -> t.getDueDate() != null ? t.getDueDate() : t.getStartDate())
                .max(Comparator.naturalOrder())
                .orElse(LocalDate.now().plusMonths(1));

        // Đảm bảo range tối thiểu 1 tháng để FE không bị viewport quá hẹp
        if (!rangeEnd.isAfter(rangeStart.plusWeeks(2))) {
            rangeEnd = rangeStart.plusMonths(1);
        }

        LocalDate today = LocalDate.now();
        long overdueCount   = tasks.stream().filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(today) && t.getCompletedAt() == null).count();
        long completedCount = tasks.stream().filter(t -> t.getStatus() == Task.TaskStatus.DONE || t.getStatus() == Task.TaskStatus.RESOLVED).count();
        long inProgressCount= tasks.stream().filter(t -> t.getStatus() == Task.TaskStatus.IN_PROGRESS || t.getStatus() == Task.TaskStatus.IN_REVIEW).count();
        long noDateCount    = tasks.stream().filter(t -> t.getStartDate() == null && t.getDueDate() == null).count();

        return GanttResponse.builder()
                .tasks(items)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .totalTasks(tasks.size())
                .overdueCount((int) overdueCount)
                .completedCount((int) completedCount)
                .inProgressCount((int) inProgressCount)
                .noDateCount((int) noDateCount)
                .build();
    }

    // Lấy ngày đại diện cho 1 task khi filter theo dateRange
    private LocalDate effectiveDate(Task t) {
        if (t.getStartDate() != null) return t.getStartDate();
        if (t.getDueDate()   != null) return t.getDueDate();
        return null;
    }
}
