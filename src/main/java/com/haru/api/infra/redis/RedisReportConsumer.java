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

    private final StringRedisTemplate redisTemplate;
    private final MoodTrackerReportService moodTrackerReportService;

    @Value("${queue-name}")
    private String QUEUE_KEY;
    private static final long BATCH_SIZE = 20;

    @Transactional
    @Scheduled(cron = "0 0/5 * * * *") // 정각부터 5분 마다 실행
    public void pollQueueEvery30Minutes() {
        long now = Instant.now().getEpochSecond();

        while (true) {
            Set<String> dueIds = redisTemplate.opsForZSet()
                    .rangeByScore(QUEUE_KEY, 0, now, 0, BATCH_SIZE);

            if (dueIds == null || dueIds.isEmpty()) break;

            for (String id : dueIds) {
                // Worker Queue로 push
                redisTemplate.opsForList().leftPush("report-worker-queue", id);
                // ZSET에서는 제거
                redisTemplate.opsForZSet().remove(QUEUE_KEY, id);
            }
        }
    }

    @Transactional
    public void removeFromQueue(Long moodTrackerId) {
        try {
            Long removed = redisTemplate.opsForZSet().remove(QUEUE_KEY, moodTrackerId.toString());
            if (removed != null && removed > 0) {
                log.info("즉시 생성 API 호출로 큐에서 제거됨: {}", moodTrackerId);
            } else {
                log.info("큐에 존재하지 않아 제거할 항목 없음: {}", moodTrackerId);
            }
        } catch (Exception e) {
            log.error("큐 제거 실패: {}", moodTrackerId, e);
        }
    }
}
