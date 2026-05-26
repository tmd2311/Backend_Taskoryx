package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.AiJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface AiJobRepository extends JpaRepository<AiJob, UUID> {

    long countByStatus(AiJob.JobStatus status);

    @Modifying
    @Query("UPDATE AiJob j SET j.status = 'FAILED', j.errorMessage = :msg, j.finishedAt = :now " +
           "WHERE j.status IN ('PENDING', 'RUNNING')")
    int markStaleJobsAsFailed(String msg, LocalDateTime now);
}
