package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.ProjectTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ProjectTemplateRepository extends JpaRepository<ProjectTemplate, UUID> {
    @Query("SELECT t FROM ProjectTemplate t WHERE t.isPublic = true OR t.createdBy.id = :userId ORDER BY t.name ASC")
    List<ProjectTemplate> findAvailableTemplates(UUID userId);

    List<ProjectTemplate> findByIsPublicTrueOrderByNameAsc();

    List<ProjectTemplate> findByCategory(String category);
}
