package com.haru.api.infra.redis;

import com.haru.api.domain.moodTracker.service.MoodTrackerReportService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisReportConsumer {

    private final MoodTrackerReportService reportService;
    private final StringRedisTemplate redisTemplate;

    @Value("${queue-name}")
    private String QUEUE_KEY;
    private static final long BATCH_SIZE = 20;

    private static final String WORKER_QUEUE = "REPORT_WORKER_QUEUE";
    private static final String FAILED_QUEUE = "REPORT_FAILED_QUEUE";

    @Scheduled(cron = "0 0/5 * * * *") // 정각부터 5분 마다 실행
    public void pollQueueEvery5Minutes() {
        long now = Instant.now().getEpochSecond();
        log.info("[RedisReportConsumer] pollQueueEvery5Minutes 실행됨 (now = {})", now);

        while (true) {
            Set<String> dueIds = redisTemplate.opsForZSet()
                    .rangeByScore(QUEUE_KEY, 0, now, 0, BATCH_SIZE);

            if (dueIds == null || dueIds.isEmpty()) {
                log.info("[RedisReportConsumer] dueIds 없음 → 반복 종료");
                break;
            }

            log.info("[RedisReportConsumer] 처리할 dueIds: {}", dueIds);

            for (String id : dueIds) {
                try {
                    // Worker Queue로 push
                    redisTemplate.opsForList().leftPush(WORKER_QUEUE, id);
                    log.info("[RedisReportConsumer] REPORT_WORKER_QUEUE에 추가됨 → {}", id);

                    // ZSET에서 제거
                    redisTemplate.opsForZSet().remove(QUEUE_KEY, id);
                    log.info("[RedisReportConsumer] ZSET({})에서 제거됨 → {}", QUEUE_KEY, id);
                } catch (Exception e) {
                    log.error("[RedisReportConsumer] id={} 처리 중 에러", id, e);
                }
            }
        }
    }

    @Scheduled(cron = "0 0/1 * * * *") // 매 1분
    public void pollFailedQueueEvery1Minutes() {
        long now = Instant.now().getEpochSecond();
        Set<String> failedIds = redisTemplate.opsForZSet()
                .rangeByScore(FAILED_QUEUE, 0, now, 0, BATCH_SIZE);

        if (failedIds == null || failedIds.isEmpty()) {
            log.info("[RedisReportConsumer] 실패 큐 없음");
            return;
        }

        for (String id : failedIds) {
            // 다시 Worker 큐로 push
            redisTemplate.opsForList().leftPush(WORKER_QUEUE, id);
            log.info("[RedisReportConsumer] 실패 큐 → 워커 큐 이동: {}", id);

            // 실패 큐에서 제거
            redisTemplate.opsForZSet().remove(FAILED_QUEUE, id);
        }
    }

    @Transactional
    public void removeFromQueue(Long moodTrackerId) {
        try {
            Long removed = redisTemplate.opsForZSet().remove(QUEUE_KEY, moodTrackerId.toString());
            if (removed != null && removed > 0) {
                log.info("[RedisReportConsumer] 즉시 생성 API → 큐에서 제거됨: {}", moodTrackerId);
            } else {
                log.info("[RedisReportConsumer] 즉시 생성 API → 큐에 없음: {}", moodTrackerId);
            }
        } catch (Exception e) {
            log.error("[RedisReportConsumer] 즉시 생성 API → 큐 제거 실패: {}", moodTrackerId, e);
        }
    }
}
