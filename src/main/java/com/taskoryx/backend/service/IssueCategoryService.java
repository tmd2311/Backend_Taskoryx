package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.request.category.CreateCategoryRequest;
import com.taskoryx.backend.dto.request.category.UpdateCategoryRequest;
import com.taskoryx.backend.dto.response.category.CategoryResponse;
import com.taskoryx.backend.entity.ActivityLog;
import com.taskoryx.backend.entity.IssueCategory;
import com.taskoryx.backend.entity.Project;
import com.taskoryx.backend.entity.ProjectPermission;
import com.taskoryx.backend.entity.User;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.IssueCategoryRepository;
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
public class IssueCategoryService {

    private final IssueCategoryRepository categoryRepository;
    private final ProjectService projectService;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    @Transactional
    public CategoryResponse createCategory(UUID projectId, CreateCategoryRequest request, UserPrincipal principal) {
        projectAuthorizationService.requirePermission(projectId, principal.getId(), ProjectPermission.CATEGORY_MANAGE);
        Project project = projectService.findProjectWithAccess(projectId, principal.getId());

        if (categoryRepository.existsByProjectIdAndName(projectId, request.getName())) {
            throw new BadRequestException("Danh mục '" + request.getName() + "' đã tồn tại trong project này");
        }

        User defaultAssignee = null;
        if (request.getDefaultAssigneeId() != null) {
            defaultAssignee = userRepository.findById(request.getDefaultAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getDefaultAssigneeId()));
        }

        IssueCategory category = IssueCategory.builder()
                .project(project)
                .name(request.getName())
                .defaultAssignee(defaultAssignee)
                .build();
        IssueCategory saved = categoryRepository.save(category);

        User actor = userRepository.findById(principal.getId()).orElseThrow();
        String createDesc = actor.getFullName() + " đã tạo danh mục \"" + saved.getName() + "\" trong dự án \"" + project.getName() + "\"";
        activityLogService.logActivity(actor, project,
                ActivityLog.EntityType.PROJECT, saved.getId(), ActivityLog.Action.CREATE,
                saved.getName(), createDesc,
                null, "{\"categoryName\":\"" + saved.getName() + "\"}");

        return CategoryResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(UUID projectId, UserPrincipal principal) {
        projectAuthorizationService.requirePermission(projectId, principal.getId(), ProjectPermission.TASK_VIEW);
        return categoryRepository.findByProjectIdOrderByNameAsc(projectId).stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponse updateCategory(UUID categoryId, UpdateCategoryRequest request, UserPrincipal principal) {
        IssueCategory category = findCategoryById(categoryId);
        projectAuthorizationService.requirePermission(category.getProject().getId(), principal.getId(),
                ProjectPermission.CATEGORY_MANAGE);
        Project project = projectService.findProjectWithAccess(category.getProject().getId(), principal.getId());

        if (request.getName() != null && !request.getName().equals(category.getName())) {
            if (categoryRepository.existsByProjectIdAndName(project.getId(), request.getName())) {
                throw new BadRequestException("Danh mục '" + request.getName() + "' đã tồn tại trong project này");
            }
            category.setName(request.getName());
        }

        if (request.isClearDefaultAssignee()) {
            category.setDefaultAssignee(null);
        } else if (request.getDefaultAssigneeId() != null) {
            User assignee = userRepository.findById(request.getDefaultAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getDefaultAssigneeId()));
            category.setDefaultAssignee(assignee);
        }

        IssueCategory updatedCategory = categoryRepository.save(category);

        User actor = userRepository.findById(principal.getId()).orElseThrow();
        String updateDesc = actor.getFullName() + " đã cập nhật danh mục \"" + updatedCategory.getName() + "\"";
        activityLogService.logActivity(actor, updatedCategory.getProject(),
                ActivityLog.EntityType.PROJECT, updatedCategory.getId(), ActivityLog.Action.UPDATE,
                updatedCategory.getName(), updateDesc,
                null, "{\"categoryName\":\"" + updatedCategory.getName() + "\"}");

        return CategoryResponse.from(updatedCategory);
    }

    @Transactional
    public void deleteCategory(UUID categoryId, UserPrincipal principal) {
        IssueCategory category = findCategoryById(categoryId);
        projectAuthorizationService.requirePermission(category.getProject().getId(), principal.getId(),
                ProjectPermission.CATEGORY_MANAGE);

        if (!category.getTasks().isEmpty()) {
            throw new BadRequestException("Không thể xóa danh mục đang có task. Hãy gỡ task khỏi danh mục trước.");
        }

        User actor = userRepository.findById(principal.getId()).orElseThrow();
        String deleteDesc = actor.getFullName() + " đã xóa danh mục \"" + category.getName() + "\" khỏi dự án \"" + category.getProject().getName() + "\"";
        activityLogService.logActivity(actor, category.getProject(),
                ActivityLog.EntityType.PROJECT, category.getId(), ActivityLog.Action.DELETE,
                category.getName(), deleteDesc,
                "{\"categoryName\":\"" + category.getName() + "\"}", null);

        categoryRepository.delete(category);
    }

    public IssueCategory findCategoryById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IssueCategory", "id", id));
    }
}
