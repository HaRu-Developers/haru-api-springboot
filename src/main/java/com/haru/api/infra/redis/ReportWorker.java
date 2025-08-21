package com.haru.api.infra.redis;

import com.haru.api.domain.moodTracker.service.MoodTrackerReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportWorker {

    private final StringRedisTemplate redisTemplate;
    private final MoodTrackerReportService reportService;
    private final ExecutorService executor = Executors.newFixedThreadPool(5); // 5개 병렬 Worker

    @Value("${queue-name}")
    private String QUEUE_KEY;
    private static final String WORKER_QUEUE = "REPORT_WORKER_QUEUE";
    private static final String FAILED_QUEUE = "REPORT_FAILED_QUEUE";

    @Scheduled(fixedDelay = 2000) // 2초마다 큐 확인
    public void consumeTasks() {
        String task = redisTemplate.opsForList().rightPop(WORKER_QUEUE);
        if (task != null) {
            Long moodTrackerId = Long.parseLong(task);
            executor.submit(() -> process(moodTrackerId));
        }
    }

    private void process(Long moodTrackerId) {
        try {
            reportService.generateAndUploadReportFileAndThumbnail(moodTrackerId);
            log.info("Report 생성 성공: {}", moodTrackerId);
            // ZSET에서 제거
            redisTemplate.opsForZSet().remove(QUEUE_KEY, moodTrackerId);
            log.info("[RedisReportConsumer] ZSET({})에서 제거됨 → {}", QUEUE_KEY, moodTrackerId);
        } catch (Exception e) {
            log.error("Report 생성 실패 (재시도 예정): {}", moodTrackerId, e);

            String key = "retry-count:" + moodTrackerId;
            Long retry = redisTemplate.opsForValue().increment(key);

            if (retry != null && retry <= 3) {
                redisTemplate.opsForList().leftPush(WORKER_QUEUE, moodTrackerId.toString());
            } else {
                log.error("재시도 한계 초과, 실패 큐로 이동: {}", moodTrackerId);
                redisTemplate.opsForList().leftPush(FAILED_QUEUE, moodTrackerId.toString());
            }
        }
    }
}
