package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.response.performance.UserPerformanceResponse;
import com.taskoryx.backend.entity.*;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.*;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceService {

    private final UserProjectPerformanceRepository performanceRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;
    private final SprintRepository sprintRepository;
    private final CommentRepository commentRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ProjectService projectService;
    private final ProjectAuthorizationService projectAuthorizationService;

    /**
     * Lấy điểm hiệu suất tất cả thành viên (project-level, không theo sprint).
     */
    @Transactional(readOnly = true)
    public List<UserPerformanceResponse> getProjectPerformance(UUID projectId, UserPrincipal principal) {
        projectAuthorizationService.requirePermission(projectId, principal.getId(), ProjectPermission.REPORT_VIEW);
        return performanceRepository.findByProjectIdAndSprintIsNullOrderByTotalScoreDesc(projectId)
                .stream()
                .map(UserPerformanceResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Lấy điểm hiệu suất của user hiện tại trong project.
     */
    @Transactional(readOnly = true)
    public UserPerformanceResponse getMyPerformance(UUID projectId, UserPrincipal principal) {
        projectAuthorizationService.requireProjectAccess(projectId, principal.getId());
        return performanceRepository.findByProjectIdAndUserIdAndSprintIsNull(projectId, principal.getId())
                .map(UserPerformanceResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Performance", "projectId", projectId));
    }

    /**
     * Tính lại điểm hiệu suất cho tất cả thành viên project (chỉ Owner/Admin).
     */
    @Transactional
    public List<UserPerformanceResponse> calculateProjectPerformance(UUID projectId, UserPrincipal principal) {
        projectAuthorizationService.requireProjectAdmin(projectId, principal.getId());
        Project project = projectService.findProjectWithAccess(projectId, principal.getId());

        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);
        if (members.isEmpty()) return List.of();

        // Build engagement map: userId → (commentCount + activityCount)
        Map<UUID, Long> engagementMap = buildEngagementMap(projectId);
        double teamAvg = engagementMap.values().stream()
                .mapToLong(Long::longValue).average().orElse(0.0);

        List<UserProjectPerformance> results = new ArrayList<>();

        for (ProjectMember member : members) {
            UUID userId = member.getUser().getId();

            List<Task> tasks = taskRepository.findByProjectIdAndAssigneeId(projectId, userId);
            int taskCount = tasks.size();
            int completedCount = (int) tasks.stream()
                    .filter(t -> t.getStatus() == Task.TaskStatus.DONE
                            || t.getStatus() == Task.TaskStatus.RESOLVED)
                    .count();
            int overdueCount = (int) tasks.stream()
                    .filter(t -> t.getDueDate() != null && t.getCompletedAt() == null
                            && t.getDueDate().isBefore(LocalDate.now()))
                    .count();

            BigDecimal onTime = calculateOnTimeScore(tasks);
            BigDecimal completion = calculateCompletionScore(tasks);
            BigDecimal timeAccuracy = calculateTimeAccuracyScore(tasks);
            BigDecimal engagement = calculateEngagementScore(userId, engagementMap, teamAvg);
            BigDecimal total = computeTotalScore(onTime, completion, timeAccuracy, engagement);

            // Upsert: tìm record cũ hoặc tạo mới
            UserProjectPerformance perf = performanceRepository
                    .findByProjectIdAndUserIdAndSprintIsNull(projectId, userId)
                    .orElseGet(() -> UserProjectPerformance.builder()
                            .user(member.getUser())
                            .project(project)
                            .build());

            perf.setOnTimeScore(onTime);
            perf.setCompletionScore(completion);
            perf.setTimeAccuracyScore(timeAccuracy);
            perf.setEngagementScore(engagement);
            perf.setTotalScore(total);
            perf.setTaskCount(taskCount);
            perf.setCompletedCount(completedCount);
            perf.setOverdueCount(overdueCount);
            perf.setEvaluatedAt(LocalDateTime.now());

            results.add(performanceRepository.save(perf));
        }

        // Gán rank theo totalScore desc
        results.sort(Comparator.comparing(
                p -> p.getTotalScore() != null ? p.getTotalScore() : BigDecimal.ZERO,
                Comparator.reverseOrder()));
        for (int i = 0; i < results.size(); i++) {
            results.get(i).setRank(i + 1);
            performanceRepository.save(results.get(i));
        }

        log.info("Đã tính điểm hiệu suất cho {} thành viên trong project {}", results.size(), projectId);
        return results.stream().map(UserPerformanceResponse::from).collect(Collectors.toList());
    }

    /**
     * Lấy điểm hiệu suất theo sprint.
     */
    @Transactional(readOnly = true)
    public List<UserPerformanceResponse> getSprintPerformance(UUID projectId, UUID sprintId,
                                                               UserPrincipal principal) {
        projectAuthorizationService.requirePermission(projectId, principal.getId(), ProjectPermission.REPORT_VIEW);
        sprintRepository.findById(sprintId)
                .filter(s -> s.getProject().getId().equals(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", "id", sprintId));
        return performanceRepository.findByProjectIdAndSprintIdOrderByTotalScoreDesc(projectId, sprintId)
                .stream()
                .map(UserPerformanceResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Lấy điểm hiệu suất của user trên tất cả project đã tham gia.
     */
    @Transactional(readOnly = true)
    public List<UserPerformanceResponse> getMyPerformanceAcrossProjects(UserPrincipal principal) {
        return performanceRepository.findByUserIdAndSprintIsNullOrderByTotalScoreDesc(principal.getId())
                .stream()
                .map(UserPerformanceResponse::from)
                .collect(Collectors.toList());
    }

    // ─── Private scoring helpers ──────────────────────────────────────────────

    /**
     * OnTimeScore: % tasks hoàn thành trước hoặc đúng deadline.
     */
    private BigDecimal calculateOnTimeScore(List<Task> tasks) {
        List<Task> withDeadline = tasks.stream()
                .filter(t -> t.getDueDate() != null && t.getCompletedAt() != null)
                .collect(Collectors.toList());
        if (withDeadline.isEmpty()) return BigDecimal.ZERO;

        long onTime = withDeadline.stream()
                .filter(t -> !t.getCompletedAt().toLocalDate().isAfter(t.getDueDate()))
                .count();
        return BigDecimal.valueOf(onTime * 100.0 / withDeadline.size())
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * CompletionScore: % tasks DONE/RESOLVED trên tổng tasks assign.
     */
    private BigDecimal calculateCompletionScore(List<Task> tasks) {
        if (tasks.isEmpty()) return BigDecimal.ZERO;
        long done = tasks.stream()
                .filter(t -> t.getStatus() == Task.TaskStatus.DONE
                        || t.getStatus() == Task.TaskStatus.RESOLVED)
                .count();
        return BigDecimal.valueOf(done * 100.0 / tasks.size())
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * TimeAccuracyScore: độ chính xác ước tính giờ.
     * accuracy = max(0, 1 - |actual - estimated| / estimated) per task
     */
    private BigDecimal calculateTimeAccuracyScore(List<Task> tasks) {
        List<Task> eligible = tasks.stream()
                .filter(t -> t.getEstimatedHours() != null
                        && t.getActualHours() != null
                        && t.getEstimatedHours().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        if (eligible.isEmpty()) return BigDecimal.ZERO;

        double sum = eligible.stream().mapToDouble(t -> {
            double est = t.getEstimatedHours().doubleValue();
            double act = t.getActualHours().doubleValue();
            return Math.max(0.0, 1.0 - Math.abs(act - est) / est);
        }).sum();

        return BigDecimal.valueOf(sum * 100.0 / eligible.size())
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * EngagementScore: normalized so với team average.
     * 50 = trung bình; >50 nếu trên TB; <50 nếu dưới TB.
     */
    private BigDecimal calculateEngagementScore(UUID userId, Map<UUID, Long> engagementMap,
                                                 double teamAvg) {
        long userTotal = engagementMap.getOrDefault(userId, 0L);
        double score;
        if (teamAvg == 0.0) {
            score = 50.0;
        } else if (userTotal >= teamAvg) {
            score = Math.min(100.0, 50.0 + (userTotal / teamAvg) * 50.0);
        } else {
            score = (userTotal / teamAvg) * 50.0;
        }
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * TotalScore = 40% onTime + 30% completion + 20% timeAccuracy + 10% engagement
     */
    private BigDecimal computeTotalScore(BigDecimal onTime, BigDecimal completion,
                                          BigDecimal timeAccuracy, BigDecimal engagement) {
        double total = onTime.doubleValue() * 0.40
                + completion.doubleValue() * 0.30
                + timeAccuracy.doubleValue() * 0.20
                + engagement.doubleValue() * 0.10;
        return BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Build engagement map: userId → commentCount + activityLogCount trong project.
     */
    private Map<UUID, Long> buildEngagementMap(UUID projectId) {
        Map<UUID, Long> map = new HashMap<>();

        for (Object[] row : commentRepository.countPerUserByProjectId(projectId)) {
            UUID uid = (UUID) row[0];
            Long count = (Long) row[1];
            map.merge(uid, count, Long::sum);
        }
        for (Object[] row : activityLogRepository.countPerUserByProjectId(projectId)) {
            UUID uid = (UUID) row[0];
            Long count = (Long) row[1];
            map.merge(uid, count, Long::sum);
        }
        return map;
    }

}
