package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.IssueCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IssueCategoryRepository extends JpaRepository<IssueCategory, UUID> {
    List<IssueCategory> findByProjectIdOrderByNameAsc(UUID projectId);
    boolean existsByProjectIdAndName(UUID projectId, String name);
}
