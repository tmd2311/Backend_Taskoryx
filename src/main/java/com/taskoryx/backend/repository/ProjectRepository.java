package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    boolean existsByKey(String key);

    Optional<Project> findByKey(String key);

    // Lấy tất cả project của user (owner hoặc member)
    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN p.members m " +
           "WHERE (p.owner.id = :userId OR m.user.id = :userId) " +
           "AND p.isArchived = false ORDER BY p.updatedAt DESC")
    List<Project> findProjectsByUserId(@Param("userId") UUID userId);

    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN p.members m " +
           "WHERE (p.owner.id = :userId OR m.user.id = :userId) " +
           "AND p.isArchived = false " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.key) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Project> searchProjectsByUserId(@Param("userId") UUID userId,
                                          @Param("keyword") String keyword,
                                          Pageable pageable);

    // Kiểm tra user có phải thành viên project không
    @Query("SELECT COUNT(pm) > 0 FROM ProjectMember pm " +
           "WHERE pm.project.id = :projectId AND pm.user.id = :userId")
    boolean isProjectMember(@Param("projectId") UUID projectId, @Param("userId") UUID userId);

    @Query("SELECT COUNT(p) > 0 FROM Project p " +
           "WHERE p.id = :projectId AND p.owner.id = :userId")
    boolean isProjectOwner(@Param("projectId") UUID projectId, @Param("userId") UUID userId);
}
