package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.TaskWatcher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskWatcherRepository extends JpaRepository<TaskWatcher, UUID> {
    List<TaskWatcher> findByTaskId(UUID taskId);
    Optional<TaskWatcher> findByTaskIdAndUserId(UUID taskId, UUID userId);
    boolean existsByTaskIdAndUserId(UUID taskId, UUID userId);
    void deleteByTaskIdAndUserId(UUID taskId, UUID userId);
    List<TaskWatcher> findByUserId(UUID userId);
    long countByTaskId(UUID taskId);
}
