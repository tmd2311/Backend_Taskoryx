package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.ProjectRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRoleRepository extends JpaRepository<ProjectRole, UUID> {

    List<ProjectRole> findByProjectIdOrderByNameAsc(UUID projectId);

    Optional<ProjectRole> findByProjectIdAndName(UUID projectId, String name);

    boolean existsByProjectIdAndName(UUID projectId, String name);

    void deleteByProjectId(UUID projectId);
}
