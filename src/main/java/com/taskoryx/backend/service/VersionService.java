package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.request.version.CreateVersionRequest;
import com.taskoryx.backend.dto.request.version.UpdateVersionRequest;
import com.taskoryx.backend.dto.response.version.RoadmapVersionItem;
import com.taskoryx.backend.dto.response.version.VersionResponse;
import com.taskoryx.backend.entity.Project;
import com.taskoryx.backend.entity.ProjectMember;
import com.taskoryx.backend.entity.Version;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.VersionRepository;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VersionService {

    private final VersionRepository versionRepository;
    private final ProjectService projectService;

    @Transactional
    public VersionResponse createVersion(UUID projectId, CreateVersionRequest request, UserPrincipal principal) {
        Project project = projectService.findProjectWithAccess(projectId, principal.getId());
        requireAdminOrOwner(project, principal.getId());

        if (versionRepository.existsByProjectIdAndName(projectId, request.getName())) {
            throw new BadRequestException("Version với tên '" + request.getName() + "' đã tồn tại trong project này");
        }

        Version version = Version.builder()
                .project(project)
                .name(request.getName())
                .description(request.getDescription())
                .dueDate(request.getDueDate())
                .releaseDate(request.getReleaseDate())
                .build();

        return VersionResponse.from(versionRepository.save(version));
    }

    @Transactional(readOnly = true)
    public List<VersionResponse> getVersions(UUID projectId, UserPrincipal principal) {
        projectService.findProjectWithAccess(projectId, principal.getId());
        return versionRepository.findByProjectIdOrderByDueDateAsc(projectId).stream()
                .map(VersionResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VersionResponse getVersion(UUID versionId, UserPrincipal principal) {
        Version version = findVersionById(versionId);
        projectService.findProjectWithAccess(version.getProject().getId(), principal.getId());
        return VersionResponse.from(version);
    }

    @Transactional
    public VersionResponse updateVersion(UUID versionId, UpdateVersionRequest request, UserPrincipal principal) {
        Version version = findVersionById(versionId);
        Project project = projectService.findProjectWithAccess(version.getProject().getId(), principal.getId());
        requireAdminOrOwner(project, principal.getId());

        if (request.getName() != null && !request.getName().equals(version.getName())) {
            if (versionRepository.existsByProjectIdAndName(project.getId(), request.getName())) {
                throw new BadRequestException("Version với tên '" + request.getName() + "' đã tồn tại trong project này");
            }
            version.setName(request.getName());
        }
        if (request.getDescription() != null) version.setDescription(request.getDescription());
        if (request.getStatus() != null) version.setStatus(request.getStatus());
        if (request.getDueDate() != null) version.setDueDate(request.getDueDate());
        if (request.getReleaseDate() != null) version.setReleaseDate(request.getReleaseDate());

        return VersionResponse.from(versionRepository.save(version));
    }

    @Transactional
    public void deleteVersion(UUID versionId, UserPrincipal principal) {
        Version version = findVersionById(versionId);
        Project project = projectService.findProjectWithAccess(version.getProject().getId(), principal.getId());
        requireAdminOrOwner(project, principal.getId());

        if (!version.getTasks().isEmpty()) {
            throw new BadRequestException("Không thể xóa version đang có task. Hãy gỡ task khỏi version trước.");
        }
        versionRepository.delete(version);
    }

    @Transactional(readOnly = true)
    public List<RoadmapVersionItem> getRoadmap(UUID projectId, UserPrincipal principal) {
        projectService.findProjectWithAccess(projectId, principal.getId());
        return versionRepository.findByProjectIdOrderByDueDateAsc(projectId).stream()
                .map(RoadmapVersionItem::from)
                .collect(Collectors.toList());
    }

    public Version findVersionById(UUID id) {
        return versionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Version", "id", id));
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
