package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    List<ProjectMember> findByProjectId(UUID projectId);

    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    void deleteByProjectIdAndUserId(UUID projectId, UUID userId);

    @Query("SELECT pm.role FROM ProjectMember pm " +
           "WHERE pm.project.id = :projectId AND pm.user.id = :userId")
    Optional<ProjectMember.ProjectRole> findRoleByProjectIdAndUserId(
            @Param("projectId") UUID projectId, @Param("userId") UUID userId);
}
