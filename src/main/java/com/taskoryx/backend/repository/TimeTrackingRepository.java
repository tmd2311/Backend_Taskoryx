package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.TimeTracking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TimeTrackingRepository extends JpaRepository<TimeTracking, UUID> {

    List<TimeTracking> findByTaskId(UUID taskId);

    List<TimeTracking> findByTaskIdAndUserId(UUID taskId, UUID userId);

    Page<TimeTracking> findByUserIdOrderByWorkDateDesc(UUID userId, Pageable pageable);

    List<TimeTracking> findByUserIdAndWorkDateBetween(UUID userId, LocalDate start, LocalDate end);

    @Query("SELECT SUM(t.hours) FROM TimeTracking t WHERE t.task.id = :taskId")
    Optional<BigDecimal> sumHoursByTaskId(@Param("taskId") UUID taskId);

    @Query("SELECT SUM(t.hours) FROM TimeTracking t WHERE t.task.project.id = :projectId")
    Optional<BigDecimal> sumHoursByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT SUM(t.hours) FROM TimeTracking t WHERE t.user.id = :userId AND t.workDate BETWEEN :start AND :end")
    Optional<BigDecimal> sumHoursByUserIdAndWorkDateBetween(
            @Param("userId") UUID userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query(value = "SELECT t FROM TimeTracking t WHERE t.task.project.id = :projectId ORDER BY t.createdAt DESC",
           countQuery = "SELECT COUNT(t) FROM TimeTracking t WHERE t.task.project.id = :projectId")
    Page<TimeTracking> findByProjectIdOrderByCreatedAtDesc(@Param("projectId") UUID projectId, Pageable pageable);
}
