package com.haru.api.domain.workspace.converter;

import com.haru.api.domain.lastOpened.entity.enums.DocumentType;
import com.haru.api.domain.meeting.entity.Meeting;
import com.haru.api.domain.moodTracker.entity.MoodTracker;
import com.haru.api.domain.snsEvent.entity.SnsEvent;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import com.haru.api.domain.workspace.entity.Workspace;

import java.time.LocalDateTime;
import java.util.List;

public class WorkspaceConverter {
    public static WorkspaceResponseDTO.Workspace toWorkspaceDTO(Workspace workspace) {
        return WorkspaceResponseDTO.Workspace.builder()
                .workspaceId(workspace.getId())
                .name(workspace.getTitle())
                .imageUrl(workspace.getImageUrl())
                .build();
    }

    public static WorkspaceResponseDTO.Document toDocument(Meeting meeting, LocalDateTime lastOpened) {
        return WorkspaceResponseDTO.Document.builder()
                .documentId(meeting.getId())
                .documentType(DocumentType.AI_MEETING_MANAGER)
                .title(meeting.getTitle())
                .lastOpened(lastOpened)
                .build();
    }

    public static WorkspaceResponseDTO.Document toDocument(SnsEvent snsEvent, LocalDateTime lastOpened) {
        return WorkspaceResponseDTO.Document.builder()
                .documentId(snsEvent.getId())
                .documentType(DocumentType.SNS_EVENT_ASSISTANT)
                .title(snsEvent.getTitle())
                .lastOpened(lastOpened)
                .build();
    }

    public static WorkspaceResponseDTO.Document toDocument(MoodTracker moodTracker, LocalDateTime lastOpened) {
        return WorkspaceResponseDTO.Document.builder()
                .documentId(moodTracker.getId())
                .documentType(DocumentType.TEAM_MOOD_TRACKER)
                .title(moodTracker.getTitle())
                .lastOpened(lastOpened)
                .build();
    }

    public static WorkspaceResponseDTO.DocumentList toDocumentsDTO(List<WorkspaceResponseDTO.Document> documentList) {
        return WorkspaceResponseDTO.DocumentList.builder()
                .documents(documentList)
                .build();
    }
}
