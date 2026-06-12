package com.taskoryx.backend.ai.service;

import com.taskoryx.backend.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window rate limiter cho AI endpoints.
 *
 * Giới hạn:
 *  - generate : tối đa 5 lần / 1 giờ / user
 *  - confirm  : tối đa 5 lần / 1 giờ / user
 *  - tổng cộng: tối đa 8 lần / 1 giờ / user (generate + confirm)
 *
 * Dùng ConcurrentHashMap + Deque timestamps — không cần dependency ngoài.
 * Dữ liệu mất khi restart server (chấp nhận được cho rate limit mềm).
 */
@Slf4j
@Component
public class AiRateLimiter {

    private static final int MAX_GENERATE_PER_HOUR = 5;
    private static final int MAX_CONFIRM_PER_HOUR  = 5;
    private static final int MAX_TOTAL_PER_HOUR    = 8;
    private static final long WINDOW_MS            = 60 * 60 * 1000L; // 1 giờ

    private final ConcurrentHashMap<UUID, Deque<Long>> generateTimestamps = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Deque<Long>> confirmTimestamps  = new ConcurrentHashMap<>();

    public void checkGenerate(UUID userId) {
        long now = System.currentTimeMillis();
        Deque<Long> genTs  = getOrCreate(generateTimestamps, userId);
        Deque<Long> confTs = getOrCreate(confirmTimestamps, userId);

        synchronized (genTs) {
            evict(genTs, now);
            if (genTs.size() >= MAX_GENERATE_PER_HOUR) {
                log.warn("AI rate limit: generate exceeded for userId={}", userId);
                throw new BadRequestException(
                    "Bạn đã sử dụng AI tạo dự án quá " + MAX_GENERATE_PER_HOUR +
                    " lần trong 1 giờ. Vui lòng thử lại sau."
                );
            }
        }

        synchronized (confTs) {
            evict(confTs, now);
            int total = genTs.size() + confTs.size();
            if (total >= MAX_TOTAL_PER_HOUR) {
                log.warn("AI rate limit: total exceeded for userId={}", userId);
                throw new BadRequestException(
                    "Bạn đã sử dụng tính năng AI quá " + MAX_TOTAL_PER_HOUR +
                    " lần trong 1 giờ. Vui lòng thử lại sau."
                );
            }
        }

        synchronized (genTs) {
            genTs.addLast(now);
        }
    }

    public void checkConfirm(UUID userId) {
        long now = System.currentTimeMillis();
        Deque<Long> genTs  = getOrCreate(generateTimestamps, userId);
        Deque<Long> confTs = getOrCreate(confirmTimestamps, userId);

        synchronized (confTs) {
            evict(confTs, now);
            if (confTs.size() >= MAX_CONFIRM_PER_HOUR) {
                log.warn("AI rate limit: confirm exceeded for userId={}", userId);
                throw new BadRequestException(
                    "Bạn đã xác nhận tạo dự án bằng AI quá " + MAX_CONFIRM_PER_HOUR +
                    " lần trong 1 giờ. Vui lòng thử lại sau."
                );
            }
        }

        synchronized (genTs) {
            evict(genTs, now);
            int total = genTs.size() + confTs.size();
            if (total >= MAX_TOTAL_PER_HOUR) {
                log.warn("AI rate limit: total exceeded for userId={}", userId);
                throw new BadRequestException(
                    "Bạn đã sử dụng tính năng AI quá " + MAX_TOTAL_PER_HOUR +
                    " lần trong 1 giờ. Vui lòng thử lại sau."
                );
            }
        }

        synchronized (confTs) {
            confTs.addLast(now);
        }
    }

    private Deque<Long> getOrCreate(ConcurrentHashMap<UUID, Deque<Long>> map, UUID userId) {
        return map.computeIfAbsent(userId, k -> new ArrayDeque<>());
    }

    // Xóa các timestamp cũ hơn cửa sổ 1 giờ
    private void evict(Deque<Long> deque, long now) {
        while (!deque.isEmpty() && now - deque.peekFirst() > WINDOW_MS) {
            deque.pollFirst();
        }
    }
}
