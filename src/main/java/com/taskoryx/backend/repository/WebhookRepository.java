package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface WebhookRepository extends JpaRepository<Webhook, UUID> {
    List<Webhook> findByProjectId(UUID projectId);
    List<Webhook> findByProjectIdAndIsActiveTrue(UUID projectId);

    @Query("SELECT w FROM Webhook w WHERE w.project.id = :projectId AND w.isActive = true AND w.events LIKE %:event%")
    List<Webhook> findActiveWebhooksForEvent(@Param("projectId") UUID projectId, @Param("event") String event);
}
