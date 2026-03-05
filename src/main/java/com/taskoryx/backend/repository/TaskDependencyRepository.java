package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.TaskDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskDependencyRepository extends JpaRepository<TaskDependency, UUID> {

    List<TaskDependency> findByTaskId(UUID taskId);

    List<TaskDependency> findByDependsOnTaskId(UUID dependsOnTaskId);

    boolean existsByTaskIdAndDependsOnTaskId(UUID taskId, UUID dependsOnTaskId);

    void deleteByTaskIdAndDependsOnTaskId(UUID taskId, UUID dependsOnTaskId);

    // Dùng cho DFS: lấy tất cả ID mà task này phụ thuộc vào
    @Query("SELECT td.dependsOnTask.id FROM TaskDependency td WHERE td.task.id = :taskId")
    List<UUID> findDependsOnTaskIdsByTaskId(@Param("taskId") UUID taskId);
}
