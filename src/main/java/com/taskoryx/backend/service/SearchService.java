package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.response.project.ProjectResponse;
import com.taskoryx.backend.dto.response.task.TaskSummaryResponse;
import com.taskoryx.backend.dto.response.user.UserResponse;
import com.taskoryx.backend.repository.ProjectRepository;
import com.taskoryx.backend.repository.TaskRepository;
import com.taskoryx.backend.repository.UserRepository;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public GlobalSearchResult search(String keyword, UserPrincipal principal) {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "updatedAt"));

        // Tìm projects
        List<ProjectResponse> projects = projectRepository
                .searchProjectsByUserId(principal.getId(), keyword, pageable)
                .map(ProjectResponse::from)
                .getContent();

        // Tìm users (để mention)
        List<UserResponse> users = userRepository.searchActiveUsers(keyword,
                PageRequest.of(0, 10)).map(UserResponse::from).getContent();

        return GlobalSearchResult.builder()
                .projects(projects)
                .users(users)
                .keyword(keyword)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TaskSummaryResponse> searchTasksInProject(String keyword, java.util.UUID projectId,
                                                           UserPrincipal principal) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new com.taskoryx.backend.exception.ResourceNotFoundException("Project", "id", projectId));

        PageRequest pageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return taskRepository.searchByProjectId(projectId, keyword, pageable)
                .map(TaskSummaryResponse::from)
                .getContent();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GlobalSearchResult {
        private String keyword;
        private List<ProjectResponse> projects;
        private List<UserResponse> users;
    }
}
