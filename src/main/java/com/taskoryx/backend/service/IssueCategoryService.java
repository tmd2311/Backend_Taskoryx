package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.request.category.CreateCategoryRequest;
import com.taskoryx.backend.dto.request.category.UpdateCategoryRequest;
import com.taskoryx.backend.dto.response.category.CategoryResponse;
import com.taskoryx.backend.entity.IssueCategory;
import com.taskoryx.backend.entity.Project;
import com.taskoryx.backend.entity.ProjectMember;
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
    private final UserRepository userRepository;

    @Transactional
    public CategoryResponse createCategory(UUID projectId, CreateCategoryRequest request, UserPrincipal principal) {
        Project project = projectService.findProjectWithAccess(projectId, principal.getId());
        requireAdminOrOwner(project, principal.getId());

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

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(UUID projectId, UserPrincipal principal) {
        projectService.findProjectWithAccess(projectId, principal.getId());
        return categoryRepository.findByProjectIdOrderByNameAsc(projectId).stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponse updateCategory(UUID categoryId, UpdateCategoryRequest request, UserPrincipal principal) {
        IssueCategory category = findCategoryById(categoryId);
        Project project = projectService.findProjectWithAccess(category.getProject().getId(), principal.getId());
        requireAdminOrOwner(project, principal.getId());

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

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(UUID categoryId, UserPrincipal principal) {
        IssueCategory category = findCategoryById(categoryId);
        Project project = projectService.findProjectWithAccess(category.getProject().getId(), principal.getId());
        requireAdminOrOwner(project, principal.getId());

        if (!category.getTasks().isEmpty()) {
            throw new BadRequestException("Không thể xóa danh mục đang có task. Hãy gỡ task khỏi danh mục trước.");
        }
        categoryRepository.delete(category);
    }

    public IssueCategory findCategoryById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IssueCategory", "id", id));
    }

    private void requireAdminOrOwner(Project project, UUID userId) {
        boolean isOwner = project.getOwner().getId().equals(userId);
        boolean isAdmin = project.getMembers().stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .anyMatch(m -> "ADMIN".equals(m.getRole()));
        if (!isOwner && !isAdmin) {
            throw new BadRequestException("Chỉ OWNER hoặc ADMIN mới có thể thực hiện thao tác này");
        }
    }
}
