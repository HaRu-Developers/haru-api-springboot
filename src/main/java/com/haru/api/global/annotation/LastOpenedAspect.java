package com.haru.api.global.annotation;

import com.haru.api.domain.lastOpened.entity.enums.DocumentType;
import com.haru.api.domain.lastOpened.service.UserDocumentLastOpenedService;
import com.haru.api.domain.meeting.dto.MeetingResponseDTO;
import com.haru.api.domain.moodTracker.dto.MoodTrackerResponseDTO;
import com.haru.api.domain.snsEvent.dto.SnsEventResponseDTO;
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
        Long documentId = Long.parseLong((String) args[documentIdIndex]);

        if (userId != null && documentId != null) {
            // document type에 따라 조회하는 repository 구분하여 workspaceId, title 추출
            Long workspaceId = null;
            String title = null;

            if(result instanceof MeetingResponseDTO.getMeetingProceeding meetingResponseDTO) {
                workspaceId = meetingResponseDTO.getWorkspaceId();
                title = meetingResponseDTO.getTitle();
            } else if(result instanceof SnsEventResponseDTO.GetSnsEventRequest snsEventResponseDTO) {
                workspaceId = Long.parseLong(snsEventResponseDTO.getWorkspaceId());
                title = snsEventResponseDTO.getTitle();
            } else if(result instanceof MoodTrackerResponseDTO.QuestionResult moodTrackerResponseDTO) {
                workspaceId = moodTrackerResponseDTO.getWorkspaceId();
                title = moodTrackerResponseDTO.getTitle();
            } else if(result instanceof MoodTrackerResponseDTO.ResponseResult moodTrackerResponseDTO) {
                workspaceId = moodTrackerResponseDTO.getWorkspaceId();
                title = moodTrackerResponseDTO.getTitle();
            } else if(result instanceof MoodTrackerResponseDTO.ReportResult moodTrackerResponseDTO) {
                workspaceId = moodTrackerResponseDTO.getWorkspaceId();
                title = moodTrackerResponseDTO.getTitle();
            }

            userDocumentLastOpenedService.updateLastOpened(userId, type, documentId, workspaceId, title);
        }

        return result;
    }

}
