package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.Version;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VersionRepository extends JpaRepository<Version, UUID> {
    List<Version> findByProjectIdOrderByDueDateAsc(UUID projectId);
    boolean existsByProjectIdAndName(UUID projectId, String name);
    long countByProjectId(UUID projectId);
}
