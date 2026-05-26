package com.taskoryx.backend.ai.service;

import com.taskoryx.backend.repository.AiGenerateSessionRepository;
import com.taskoryx.backend.repository.AiJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Khi server khởi động lại, các session/job đang GENERATING/PENDING/RUNNING
 * trong DB sẽ không bao giờ hoàn thành (semaphore đã mất). Mark chúng là FAILED
 * để FE không poll mãi mãi.
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class AiStartupCleanup implements ApplicationRunner {

    private final AiGenerateSessionRepository sessionRepository;
    private final AiJobRepository jobRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        LocalDateTime now = LocalDateTime.now();
        String reason = "Server restarted — tác vụ bị gián đoạn";

        int sessions = sessionRepository.markStaleGeneratingAsFailed(reason, now);
        int jobs = jobRepository.markStaleJobsAsFailed(reason, now);

        if (sessions > 0 || jobs > 0) {
            log.warn("AI startup cleanup: {} stale session(s) and {} stale job(s) marked as FAILED",
                    sessions, jobs);
        }
    }
}
