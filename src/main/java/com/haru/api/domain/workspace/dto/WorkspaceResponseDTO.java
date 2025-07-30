package com.haru.api.domain.workspace.dto;

import com.haru.api.domain.lastOpened.entity.enums.DocumentType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class WorkspaceResponseDTO {

    @Getter
    @Builder
    public static class Workspace {
        private Long workspaceId;
        private String title;
        private String imageUrl;
    }

    @Getter
    @Builder
    public static class Document {
        private Long documentId;
        private String title;
        private DocumentType documentType;
        private LocalDateTime lastOpened;
    }

    @Getter
    @Builder
    public static class DocumentList {
        private List<Document> documents;
    }

    @Getter
    @Builder
    public static class InvitationAcceptResult {
        private boolean isSuccess;
        private boolean isAlreadyRegistered;
        private Long workspaceId;
    }
}
