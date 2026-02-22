package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {
    List<UserRole> findByUserId(UUID userId);
    Optional<UserRole> findByUserIdAndRoleId(UUID userId, UUID roleId);
    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);
    void deleteByUserIdAndRoleId(UUID userId, UUID roleId);

    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.role r JOIN FETCH r.permissions WHERE ur.user.id = :userId")
    List<UserRole> findByUserIdWithPermissions(UUID userId);
}
