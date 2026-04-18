package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.response.gantt.GanttTaskItem;
import com.taskoryx.backend.entity.ProjectPermission;
import com.taskoryx.backend.repository.TaskRepository;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GanttService {

    private final TaskRepository taskRepository;
    private final ProjectAuthorizationService projectAuthorizationService;

    @Transactional(readOnly = true)
    public List<GanttTaskItem> getGanttData(UUID projectId, UserPrincipal principal) {
        projectAuthorizationService.requirePermission(projectId, principal.getId(), ProjectPermission.TASK_VIEW);

        // Get all tasks with dates, filter those with at least startDate or dueDate
        return taskRepository.findByProjectId(projectId, PageRequest.of(0, 1000))
                .getContent()
                .stream()
                .filter(t -> t.getStartDate() != null || t.getDueDate() != null)
                .sorted((a, b) -> {
                    if (a.getStartDate() == null && b.getStartDate() == null) return 0;
                    if (a.getStartDate() == null) return 1;
                    if (b.getStartDate() == null) return -1;
                    return a.getStartDate().compareTo(b.getStartDate());
                })
                .map(GanttTaskItem::from)
                .collect(Collectors.toList());
    }
}
