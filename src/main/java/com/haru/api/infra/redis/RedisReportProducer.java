package com.haru.api.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class RedisReportProducer {

    private final StringRedisTemplate redisTemplate;

    @Value("${queue-name}")
    private String QUEUE_KEY;

    public void scheduleReport(Long moodTrackerId, LocalDateTime dueDate) {
        long score = dueDate.atZone(ZoneId.systemDefault()).toEpochSecond();
        redisTemplate.opsForZSet().add(QUEUE_KEY, String.valueOf(moodTrackerId), score);
    }
}