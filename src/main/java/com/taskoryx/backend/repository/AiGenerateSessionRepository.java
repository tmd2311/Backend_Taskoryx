package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.AiGenerateSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface AiGenerateSessionRepository extends JpaRepository<AiGenerateSession, UUID> {

    @Modifying
    @Query("UPDATE AiGenerateSession s SET s.status = 'FAILED', s.errorMessage = :msg, s.finishedAt = :now " +
           "WHERE s.status = 'GENERATING'")
    int markStaleGeneratingAsFailed(String msg, LocalDateTime now);
}
