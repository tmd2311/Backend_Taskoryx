package com.taskoryx.backend.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskoryx.backend.ai.dto.request.AiConfirmPlanRequest;
import com.taskoryx.backend.ai.dto.response.AiJobResponse;
import com.taskoryx.backend.entity.AiJob;
import com.taskoryx.backend.entity.User;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.AiJobRepository;
import com.taskoryx.backend.repository.UserRepository;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiJobService {

    private final AiJobRepository aiJobRepository;
    private final AiJobAsyncRunner asyncRunner;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final AiRateLimiter rateLimiter;

    /**
     * Tạo job và enqueue — trả về ngay (HTTP 202).
     * Chỉ 1 job chạy cùng lúc, các job khác xếp hàng chờ semaphore trong AiJobAsyncRunner.
     */
    @Transactional
    public AiJobResponse enqueueJob(AiConfirmPlanRequest request, UserPrincipal principal) {
        rateLimiter.checkConfirm(principal.getId());

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user"));

        String planJson;
        try {
            planJson = objectMapper.writeValueAsString(request.getPlan());
        } catch (Exception e) {
            throw new BadRequestException("Không thể serialize kế hoạch AI");
        }

        AiJob job = AiJob.builder()
                .user(user)
                .status(AiJob.JobStatus.PENDING)
                .planJson(planJson)
                .targetProjectId(request.getTargetProjectId())
                .build();

        job = aiJobRepository.save(job);
        log.info("AI job enqueued: jobId={}, userId={}", job.getId(), principal.getId());

        // Chạy sau khi transaction commit để async thread thấy job trong DB
        final UUID jobId = job.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                asyncRunner.run(jobId, principal);
            }
        });

        return AiJobResponse.from(job);
    }

    /**
     * Poll trạng thái job.
     */
    @Transactional(readOnly = true)
    public AiJobResponse getJobStatus(UUID jobId, UserPrincipal principal) {
        AiJob job = aiJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy AI job"));
        if (!job.getUser().getId().equals(principal.getId())) {
            throw new ResourceNotFoundException("Không tìm thấy AI job");
        }
        return AiJobResponse.from(job);
    }
}
