package com.haru.api.infra.redis;

import com.haru.api.domain.moodTracker.service.MoodTrackerReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private static final String QUEUE_KEY = "MOOD_TRACKER_REPORT_GPT_QUEUE";
    private static final long BATCH_SIZE = 20;

    @Scheduled(cron = "0 0/30 * * * *") // 매시 0분, 30분 실행
    public void pollQueueEvery30Minutes() {
        long now = Instant.now().getEpochSecond();

        Set<String> dueIds = redisTemplate.opsForZSet()
                .rangeByScore(QUEUE_KEY, 0, now, 0, BATCH_SIZE);

        if (dueIds == null || dueIds.isEmpty()) return;

        for (String moodTrackerId : dueIds) {
            try {
                moodTrackerReportService.generateReport(Long.parseLong(moodTrackerId));
                redisTemplate.opsForZSet().remove(QUEUE_KEY, moodTrackerId);
            } catch (Exception e) {
                log.error("GPT 리포트 생성 실패: {}", moodTrackerId, e);
            }
        }
    }
}
