package com.haru.api.infra.redis;

import com.haru.api.domain.moodTracker.service.MoodTrackerReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportWorker {

    private final StringRedisTemplate redisTemplate;
    private final MoodTrackerReportService reportService;
    private final ExecutorService executor = Executors.newFixedThreadPool(5); // 5개 병렬 Worker

    private static final String WORKER_QUEUE_KEY = "REPORT_WORKER_QUEUE";
    private static final String FAILED_QUEUE_KEY = "REPORT_FAILED_QUEUE";

    @Scheduled(fixedDelay = 2000) // 2초마다 큐 확인
    public void consumeTasks() {
        String task = redisTemplate.opsForList().rightPop(WORKER_QUEUE_KEY);
        if (task != null) {
            Long moodTrackerId = Long.parseLong(task);
            executor.submit(() -> process(moodTrackerId));
        }
    }

    private void process(Long moodTrackerId) {
        try {
            reportService.generateAndUploadReportFileAndThumbnail(moodTrackerId);
            log.info("Report 생성 성공: {}", moodTrackerId);

        } catch (Exception e) {
            log.error("Report 생성 실패 (재시도 예정): {}", moodTrackerId, e);

            String key = "retry-count:" + moodTrackerId;
            Long retry = redisTemplate.opsForValue().increment(key);

            if (retry != null && retry <= 3) {
                redisTemplate.opsForList().leftPush(WORKER_QUEUE_KEY, moodTrackerId.toString());
            } else {
                long retryTime = Instant.now().plusSeconds(60).getEpochSecond();
                redisTemplate.opsForZSet().add(FAILED_QUEUE_KEY, moodTrackerId.toString(), retryTime);
                log.error("재시도 한계 초과, 실패 큐(ZSET)로 이동: {}", moodTrackerId);
            }
        }
    }
}
