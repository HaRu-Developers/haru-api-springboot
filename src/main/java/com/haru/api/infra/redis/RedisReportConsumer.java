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

    private static final String WORKER_QUEUE_KEY = "REPORT_WORKER_QUEUE";
    private static final String FAILED_QUEUE_KEY = "REPORT_FAILED_QUEUE";

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
                    redisTemplate.opsForList().leftPush(WORKER_QUEUE_KEY, id);
                    log.info("[RedisReportConsumer] WORKER_QUEUE에 추가됨 → {}", id);

                    // 본 큐에서 제거
                    redisTemplate.opsForZSet().remove(QUEUE_KEY, id);
                    log.info("[RedisReportConsumer] ZSET({})에서 제거됨 → {}", QUEUE_KEY, id);

                } catch (Exception e) {
                    log.error("[RedisReportConsumer] id={} 처리 중 에러 → 실패 큐로 이동", id, e);

                    // 실패 큐에 now+60초 후 재실행 스코어로 push
                    long retryAt = Instant.now().getEpochSecond() + 60;
                    redisTemplate.opsForZSet().add(FAILED_QUEUE_KEY, id, retryAt);
                }
            }
        }
    }

    @Scheduled(cron = "0 0/1 * * * *") // 1분마다 실행
    public void pollFailedQueueEvery1Minutes() {
        long now = Instant.now().getEpochSecond();
        log.info("[RedisReportConsumer] pollFailedQueueEvery1Minutes 실행됨 (now = {})", now);

        while (true) {
            Set<String> dueIds = redisTemplate.opsForZSet()
                    .rangeByScore(FAILED_QUEUE_KEY, 0, now, 0, BATCH_SIZE);

            if (dueIds == null || dueIds.isEmpty()) {
                log.info("[RedisReportConsumer] 실패 큐 dueIds 없음 → 반복 종료");
                break;
            }

            log.info("[RedisReportConsumer] 실패 큐 처리할 dueIds: {}", dueIds);

            for (String id : dueIds) {
                try {
                    redisTemplate.opsForList().leftPush(WORKER_QUEUE_KEY, id);
                    log.info("[RedisReportConsumer] 실패 큐 → WORKER_QUEUE에 추가됨 → {}", id);

                    redisTemplate.opsForZSet().remove(FAILED_QUEUE_KEY, id);
                    log.info("[RedisReportConsumer] 실패 큐에서 제거됨 → {}", id);

                } catch (Exception e) {
                    log.error("[RedisReportConsumer] 실패 큐 id={} 재처리 실패", id, e);

                    // 재시도는 다시 실패 큐에 넣되, 약간 늦춰서
                    long retryAt = Instant.now().getEpochSecond() + 60;
                    redisTemplate.opsForZSet().add(FAILED_QUEUE_KEY, id, retryAt);
                }
            }
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
