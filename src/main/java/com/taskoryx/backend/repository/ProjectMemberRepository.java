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
    Optional<String> findRoleByProjectIdAndUserId(
            @Param("projectId") UUID projectId, @Param("userId") UUID userId);

    /** Tìm kiếm thành viên trong project theo username/fullName/email */
    @Query("SELECT pm FROM ProjectMember pm JOIN FETCH pm.user u " +
           "WHERE pm.project.id = :projectId " +
           "AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<ProjectMember> searchMembersByKeyword(
            @Param("projectId") UUID projectId,
            @Param("keyword") String keyword);
}
