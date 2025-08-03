package com.haru.api.global.annotation;

import com.haru.api.domain.lastOpened.entity.enums.DocumentType;
import com.haru.api.domain.lastOpened.service.UserDocumentLastOpenedService;
import com.haru.api.domain.meeting.entity.Meeting;
import com.haru.api.domain.meeting.repository.MeetingRepository;
import com.haru.api.domain.moodTracker.entity.MoodTracker;
import com.haru.api.domain.moodTracker.repository.MoodTrackerRepository;
import com.haru.api.domain.snsEvent.entity.SnsEvent;
import com.haru.api.domain.snsEvent.repository.SnsEventRepository;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.MeetingHandler;
import com.haru.api.global.apiPayload.exception.handler.MoodTrackerHandler;
import com.haru.api.global.apiPayload.exception.handler.SnsEventHandler;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Order(1)
public class LastOpenedAspect {

    private final UserDocumentLastOpenedService userDocumentLastOpenedService;
    private final MeetingRepository meetingRepository;
    private final SnsEventRepository snsEventRepository;
    private final MoodTrackerRepository moodTrackerRepository;

    @Around("@annotation(trackLastOpened)")
    public Object trackLastOpened(ProceedingJoinPoint joinPoint, TrackLastOpened trackLastOpened) throws Throwable {
        // 실제 메서드 실행
        Object result = joinPoint.proceed();

        DocumentType type = trackLastOpened.type();
        int userIdIndex = trackLastOpened.userIdIndex();
        int documentIdIndex = trackLastOpened.documentIdIndex();

        // 메서드의 인자에서 userId와 documentId 추출
        Object[] args = joinPoint.getArgs();

        // 인덱스를 사용하여 userId와 documentId 추출
        Long userId = (Long) args[userIdIndex];
        Long documentId = (Long) args[documentIdIndex];

        if (userId != null && documentId != null) {
            // document type에 따라 조회하는 repository 구분하여 workspaceId, title 추출
            Long workspaceId = null;
            String title = null;

            if(type.equals(DocumentType.AI_MEETING_MANAGER)) {
                Meeting foundMeeting = meetingRepository.findById(documentId)
                        .orElseThrow(() -> new MeetingHandler(ErrorStatus.MEETING_NOT_FOUND));
                workspaceId = foundMeeting.getWorkspace().getId();
                title = foundMeeting.getTitle();
            } else if(type.equals(DocumentType.SNS_EVENT_ASSISTANT)) {
                SnsEvent foundSnsEvent = snsEventRepository.findById(documentId)
                        .orElseThrow(() -> new SnsEventHandler(ErrorStatus.SNS_EVENT_NOT_FOUND));
                workspaceId = foundSnsEvent.getWorkspace().getId();
                title = foundSnsEvent.getTitle();
            } else {
                MoodTracker foundMoodTracker = moodTrackerRepository.findById(documentId)
                        .orElseThrow(() -> new MoodTrackerHandler(ErrorStatus.MOOD_TRACKER_NOT_FOUND));
                workspaceId = foundMoodTracker.getWorkspace().getId();
                title = foundMoodTracker.getTitle();
            }

            userDocumentLastOpenedService.updateLastOpened(userId, type, documentId, workspaceId, title);
        }

        return result;
    }

}
