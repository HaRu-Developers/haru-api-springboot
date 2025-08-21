package com.haru.api.domain.workspace.converter;

import com.haru.api.domain.lastOpened.entity.UserDocumentLastOpened;
import com.haru.api.domain.lastOpened.entity.enums.DocumentType;
import com.haru.api.domain.meeting.entity.Meeting;
import com.haru.api.domain.moodTracker.entity.MoodTracker;
import com.haru.api.domain.snsEvent.entity.SnsEvent;
import com.haru.api.domain.user.dto.UserResponseDTO;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.global.util.HashIdUtil;
import com.haru.api.infra.s3.AmazonS3Manager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WorkspaceConverter {

    private final HashIdUtil hashIdUtil;
    private final AmazonS3Manager s3Manager;

    public static WorkspaceResponseDTO.Workspace toWorkspaceDTO(Workspace workspace, String presignedUrl) {
        return WorkspaceResponseDTO.Workspace.builder()
                .workspaceId(workspace.getId())
                .title(workspace.getTitle())
                .imageUrl(presignedUrl)
                .build();
    }

    public WorkspaceResponseDTO.Document toDocument(UserDocumentLastOpened document) {
        String documentId;
        if (DocumentType.TEAM_MOOD_TRACKER.equals(document.getId().getDocumentType())) {
            documentId = hashIdUtil.encode(document.getId().getDocumentId());
        } else {
            documentId = String.valueOf(document.getId().getDocumentId());
        }

        return WorkspaceResponseDTO.Document.builder()
                .documentId(documentId)
                .title(document.getTitle())
                .documentType(document.getId().getDocumentType())
                .lastOpened(document.getLastOpened())
                .build();
    }

    public static WorkspaceResponseDTO.DocumentList toDocumentList(List<WorkspaceResponseDTO.Document> documentList) {
        return WorkspaceResponseDTO.DocumentList.builder()
                .documents(documentList)
                .build();
    }

    public static WorkspaceResponseDTO.InvitationAcceptResult toInvitationAcceptResult(boolean isSuccess, boolean isAlreadyRegistered, Workspace workspace) {
        return WorkspaceResponseDTO.InvitationAcceptResult.builder()
                .isSuccess(isSuccess)
                .isAlreadyRegistered(isAlreadyRegistered)
                .workspaceId(workspace.getId())
                .build();
    }

    public WorkspaceResponseDTO.DocumentSidebar toDocumentSidebar(UserDocumentLastOpened document) {
        String documentId;
        if (DocumentType.TEAM_MOOD_TRACKER.equals(document.getId().getDocumentType())) {
            documentId = hashIdUtil.encode(document.getId().getDocumentId());
        } else {
            documentId = String.valueOf(document.getId().getDocumentId());
        }

        return WorkspaceResponseDTO.DocumentSidebar.builder()
                .documentId(documentId)
                .documentType(document.getId().getDocumentType())
                .title(document.getTitle())
                .build();
    }

    public static WorkspaceResponseDTO.DocumentSidebarList toDocumentSidebarList(List<WorkspaceResponseDTO.DocumentSidebar> documentList) {
        return WorkspaceResponseDTO.DocumentSidebarList.builder()
                .documents(documentList)
                .build();
    }

    // Meeting 엔티티 리스트를 DocumentCalendar DTO 리스트로 변환
    public List<WorkspaceResponseDTO.DocumentCalendar> toDocumentCalendarListFromMeeting(List<Meeting> meetingList) {
        return meetingList.stream()
                .map(meeting -> WorkspaceResponseDTO.DocumentCalendar.builder()
                        .documentId(String.valueOf(meeting.getId()))
                        .title(meeting.getTitle())
                        .documentType(DocumentType.AI_MEETING_MANAGER)
                        .createdAt(meeting.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // SnsEvent 엔티티 리스트를 DocumentCalendar DTO 리스트로 변환
    public List<WorkspaceResponseDTO.DocumentCalendar> toDocumentCalendarListFromSnsEvent(List<SnsEvent> snsEventList) {
        return snsEventList.stream()
                .map(snsEvent -> WorkspaceResponseDTO.DocumentCalendar.builder()
                        .documentId(String.valueOf(snsEvent.getId()))
                        .title(snsEvent.getTitle())
                        .documentType(DocumentType.SNS_EVENT_ASSISTANT)
                        .createdAt(snsEvent.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // MoodTracker 엔티티 리스트를 DocumentCalendar DTO 리스트로 변환
    public List<WorkspaceResponseDTO.DocumentCalendar> toDocumentCalendarListFromMoodTracker(List<MoodTracker> moodTrackerList) {
        return moodTrackerList.stream()
                .map(moodTracker -> WorkspaceResponseDTO.DocumentCalendar.builder()
                        .documentId(hashIdUtil.encode(moodTracker.getId())) // MoodTracker는 hashid로 변환
                        .title(moodTracker.getTitle())
                        .documentType(DocumentType.TEAM_MOOD_TRACKER)
                        .createdAt(moodTracker.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // 모든 문서 리스트를 합쳐 최종 DTO로 반환하는 통합 메서드
    public WorkspaceResponseDTO.DocumentCalendarList toDocumentCalendarList(
            List<Meeting> meetingList,
            List<SnsEvent> snsEventList,
            List<MoodTracker> moodTrackerList) {

        List<WorkspaceResponseDTO.DocumentCalendar> allResults = new ArrayList<>();
        allResults.addAll(toDocumentCalendarListFromMeeting(meetingList));
        allResults.addAll(toDocumentCalendarListFromSnsEvent(snsEventList));
        allResults.addAll(toDocumentCalendarListFromMoodTracker(moodTrackerList));

        return WorkspaceResponseDTO.DocumentCalendarList.builder()
                .documentList(allResults)
                .build();
    }

    public WorkspaceResponseDTO.WorkspaceEditPage toWorkspaceEditPage(Workspace workspace, List<UserResponseDTO.MemberInfo> memberInfoList, String imageUrl) {
        return WorkspaceResponseDTO.WorkspaceEditPage.builder()
                .title(workspace.getTitle())
                .imageUrl(imageUrl)
                .members(memberInfoList)
                .build();
    }

    public WorkspaceResponseDTO.RecentDocument toRecentDocument(UserDocumentLastOpened userDocumentLastOpened) {
        String thumbnailUrl = s3Manager.generatePresignedUrl(userDocumentLastOpened.getThumbnailKeyName());

        String documentId;

        if(userDocumentLastOpened.getId().getDocumentType().equals(DocumentType.TEAM_MOOD_TRACKER)) {
            documentId = hashIdUtil.encode(userDocumentLastOpened.getId().getDocumentId());
        } else {
            documentId = String.valueOf(userDocumentLastOpened.getId().getDocumentId());
        }

        return WorkspaceResponseDTO.RecentDocument.builder()
                .documentId(documentId)
                .title(userDocumentLastOpened.getTitle())
                .documentType(userDocumentLastOpened.getId().getDocumentType())
                .thumbnailUrl(thumbnailUrl)
                .lastOpened(userDocumentLastOpened.getLastOpened())
                .build();
    }

    public WorkspaceResponseDTO.RecentDocumentList toRecentDocumentList(List<WorkspaceResponseDTO.RecentDocument> recentDocumentList) {
        return WorkspaceResponseDTO.RecentDocumentList.builder()
                .documents(recentDocumentList)
                .build();
    }
}
