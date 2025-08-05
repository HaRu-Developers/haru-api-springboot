package com.haru.api.domain.workspace.converter;

import com.haru.api.domain.lastOpened.entity.UserDocumentLastOpened;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import com.haru.api.domain.workspace.entity.Workspace;

import java.util.List;

public class WorkspaceConverter {
    public static WorkspaceResponseDTO.Workspace toWorkspaceDTO(Workspace workspace) {
        return WorkspaceResponseDTO.Workspace.builder()
                .workspaceId(workspace.getId())
                .title(workspace.getTitle())
                .imageUrl(workspace.getImageUrl())
                .build();
    }

    public static WorkspaceResponseDTO.Document toDocument(UserDocumentLastOpened document) {
        return WorkspaceResponseDTO.Document.builder()
                .documentId(document.getId().getDocumentId())
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

    public static WorkspaceResponseDTO.DocumentSidebar toDocumentSidebar(UserDocumentLastOpened document) {
        return WorkspaceResponseDTO.DocumentSidebar.builder()
                .documentId(document.getId().getDocumentId())
                .documentType(document.getId().getDocumentType())
                .title(document.getTitle())
                .build();
    }

    public static WorkspaceResponseDTO.DocumentSidebarList toDocumentSidebarList(List<WorkspaceResponseDTO.DocumentSidebar> documentList) {
        return WorkspaceResponseDTO.DocumentSidebarList.builder()
                .documents(documentList)
                .build();
    }

    public static WorkspaceResponseDTO.DocumentCalendarList toDocumentCalendarList(List<WorkspaceResponseDTO.DocumentCalendar> documentList) {
        return WorkspaceResponseDTO.DocumentCalendarList.builder()
                .documentList(documentList)
                .build();
    }
}
