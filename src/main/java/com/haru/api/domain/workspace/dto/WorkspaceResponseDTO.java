package com.haru.api.domain.workspace.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class WorkspaceResponseDTO {

    @Getter
    @Builder
    public static class Workspace {
        private Long workspaceId;
        private String name;
        private String imageUrl;
    }

    @Getter
    @Builder
    public static class Document {
        private Long documentId;
        private String title;
        private String documentType;
        private LocalDateTime lastOpened;
    }

    @Getter
    @Builder
    public static class Documents {
        private List<Document> documentList;
    }
}
