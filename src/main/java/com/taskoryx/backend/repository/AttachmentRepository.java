package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    List<Attachment> findByTaskIdOrderByCreatedAtDesc(UUID taskId);

    long countByTaskId(UUID taskId);
}
