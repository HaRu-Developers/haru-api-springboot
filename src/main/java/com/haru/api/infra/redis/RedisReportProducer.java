package com.haru.api.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class RedisReportProducer {

    private final StringRedisTemplate redisTemplate;
    private static final String QUEUE_KEY = "MOOD_TRACKER_REPORT_GPT_QUEUE";

    public void scheduleReport(Long moodTrackerId, LocalDateTime dueDate) {
        long score = dueDate.atZone(ZoneId.systemDefault()).toEpochSecond();
        redisTemplate.opsForZSet().add(QUEUE_KEY, String.valueOf(moodTrackerId), score);
    }
}