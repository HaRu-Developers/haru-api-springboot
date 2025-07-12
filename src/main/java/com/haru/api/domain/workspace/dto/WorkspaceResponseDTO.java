package com.haru.api.domain.workspace.dto;

import lombok.Builder;
import lombok.Getter;

public class WorkspaceResponseDTO {

    @Getter
    @Builder
    public static class WorkspaceDTO {
        private Long workspaceId;
        private String name;
    }
}
