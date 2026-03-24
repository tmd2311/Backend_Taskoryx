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

    // ========== STATISTICS QUERIES ==========

    /** Lấy tất cả entries của user trong khoảng ngày (eager load task + project) */
    @Query("SELECT t FROM TimeTracking t JOIN FETCH t.task tk JOIN FETCH tk.project " +
           "WHERE t.user.id = :userId AND t.workDate BETWEEN :start AND :end " +
           "ORDER BY t.workDate ASC")
    List<TimeTracking> findByUserAndDateRangeWithTask(
            @Param("userId") UUID userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    /** Tổng giờ theo từng ngày của user */
    @Query("SELECT t.workDate, SUM(t.hours) FROM TimeTracking t " +
           "WHERE t.user.id = :userId AND t.workDate BETWEEN :start AND :end " +
           "GROUP BY t.workDate ORDER BY t.workDate ASC")
    List<Object[]> sumHoursByDayForUser(
            @Param("userId") UUID userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    /** Tổng giờ theo project của user trong khoảng ngày */
    @Query("SELECT tk.project.id, tk.project.name, tk.project.key, SUM(t.hours), COUNT(t) " +
           "FROM TimeTracking t JOIN t.task tk " +
           "WHERE t.user.id = :userId AND t.workDate BETWEEN :start AND :end " +
           "GROUP BY tk.project.id, tk.project.name, tk.project.key " +
           "ORDER BY SUM(t.hours) DESC")
    List<Object[]> sumHoursByProjectForUser(
            @Param("userId") UUID userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    /** Tổng giờ theo member trong project */
    @Query("SELECT t.user.id, t.user.fullName, t.user.avatarUrl, SUM(t.hours), COUNT(t) " +
           "FROM TimeTracking t JOIN t.task tk " +
           "WHERE tk.project.id = :projectId AND t.workDate BETWEEN :start AND :end " +
           "GROUP BY t.user.id, t.user.fullName, t.user.avatarUrl " +
           "ORDER BY SUM(t.hours) DESC")
    List<Object[]> sumHoursByMemberForProject(
            @Param("projectId") UUID projectId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    /** Tổng giờ theo task trong project */
    @Query("SELECT tk.id, tk.taskNumber, tk.title, tk.status, tk.estimatedHours, SUM(t.hours), COUNT(t) " +
           "FROM TimeTracking t JOIN t.task tk " +
           "WHERE tk.project.id = :projectId AND t.workDate BETWEEN :start AND :end " +
           "GROUP BY tk.id, tk.taskNumber, tk.title, tk.status, tk.estimatedHours " +
           "ORDER BY SUM(t.hours) DESC")
    List<Object[]> sumHoursByTaskForProject(
            @Param("projectId") UUID projectId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    /** Tổng giờ theo ngày trong project */
    @Query("SELECT t.workDate, SUM(t.hours), COUNT(t) " +
           "FROM TimeTracking t JOIN t.task tk " +
           "WHERE tk.project.id = :projectId AND t.workDate BETWEEN :start AND :end " +
           "GROUP BY t.workDate ORDER BY t.workDate ASC")
    List<Object[]> sumHoursByDayForProject(
            @Param("projectId") UUID projectId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    /** Lấy entries của project trong khoảng ngày (để build daily detail) */
    @Query("SELECT t FROM TimeTracking t JOIN FETCH t.task tk JOIN FETCH tk.project " +
           "JOIN FETCH t.user " +
           "WHERE tk.project.id = :projectId AND t.workDate BETWEEN :start AND :end " +
           "ORDER BY t.workDate ASC, t.createdAt ASC")
    List<TimeTracking> findByProjectAndDateRange(
            @Param("projectId") UUID projectId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    /** Tổng giờ theo tháng của user trong một năm */
    @Query("SELECT MONTH(t.workDate), SUM(t.hours), COUNT(t) " +
           "FROM TimeTracking t " +
           "WHERE t.user.id = :userId AND YEAR(t.workDate) = :year " +
           "GROUP BY MONTH(t.workDate) ORDER BY MONTH(t.workDate) ASC")
    List<Object[]> sumHoursByMonthForUser(
            @Param("userId") UUID userId,
            @Param("year") int year);

    /** Đếm số task phân biệt đã log trong project + khoảng ngày */
    @Query("SELECT COUNT(DISTINCT t.task.id) FROM TimeTracking t JOIN t.task tk " +
           "WHERE tk.project.id = :projectId AND t.workDate BETWEEN :start AND :end")
    long countDistinctTasksForProject(
            @Param("projectId") UUID projectId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}
