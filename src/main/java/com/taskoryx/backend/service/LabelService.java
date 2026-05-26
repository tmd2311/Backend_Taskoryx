package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.request.label.CreateLabelRequest;
import com.taskoryx.backend.dto.response.label.LabelResponse;
import com.taskoryx.backend.entity.ActivityLog;
import com.taskoryx.backend.entity.Label;
import com.taskoryx.backend.entity.ProjectPermission;
import com.taskoryx.backend.entity.User;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.LabelRepository;
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
public class LabelService {

    private final LabelRepository labelRepository;
    private final ProjectService projectService;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<LabelResponse> getLabels(UUID projectId, UserPrincipal principal) {
        projectAuthorizationService.requirePermission(projectId, principal.getId(), ProjectPermission.TASK_VIEW);
        return labelRepository.findByProjectIdOrderByNameAsc(projectId)
                .stream()
                .map(LabelResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public LabelResponse createLabel(UUID projectId, CreateLabelRequest request, UserPrincipal principal) {
        projectAuthorizationService.requirePermission(projectId, principal.getId(), ProjectPermission.LABEL_MANAGE);
        var project = projectService.findProjectWithAccess(projectId, principal.getId());

        if (labelRepository.existsByProjectIdAndName(projectId, request.getName())) {
            throw new BadRequestException("Nhãn '" + request.getName() + "' đã tồn tại trong dự án");
        }

        Label label = Label.builder()
                .project(project)
                .name(request.getName())
                .color(request.getColor())
                .description(request.getDescription())
                .build();
        Label saved = labelRepository.save(label);

        User actor = userRepository.findById(principal.getId()).orElseThrow();
        String createDesc = actor.getFullName() + " đã tạo nhãn \"" + saved.getName() + "\" trong dự án \"" + project.getName() + "\"";
        activityLogService.logActivity(actor, project,
                ActivityLog.EntityType.PROJECT, saved.getId(), ActivityLog.Action.CREATE,
                saved.getName(), createDesc,
                null, "{\"labelName\":\"" + saved.getName() + "\",\"color\":\"" + saved.getColor() + "\"}");

        return LabelResponse.from(saved);
    }

    @Transactional
    public void deleteLabel(UUID labelId, UserPrincipal principal) {
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new ResourceNotFoundException("Label", "id", labelId));
        projectAuthorizationService.requirePermission(label.getProject().getId(), principal.getId(),
                ProjectPermission.LABEL_MANAGE);

        User actor = userRepository.findById(principal.getId()).orElseThrow();
        String deleteDesc = actor.getFullName() + " đã xóa nhãn \"" + label.getName() + "\" khỏi dự án \"" + label.getProject().getName() + "\"";
        activityLogService.logActivity(actor, label.getProject(),
                ActivityLog.EntityType.PROJECT, label.getId(), ActivityLog.Action.DELETE,
                label.getName(), deleteDesc,
                "{\"labelName\":\"" + label.getName() + "\"}", null);

        labelRepository.delete(label);
    }
}
