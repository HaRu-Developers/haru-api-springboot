package com.haru.api.domain.userWorkspace.dto;

import lombok.Builder;
import lombok.Getter;

public class UserWorkspaceResponseDTO {

    @Getter
    @Builder
    public static class UserWorkspaceWithTitle {
        private Long workspaceId;
        private String title;
        private Boolean isOwner;

        public UserWorkspaceWithTitle(Long workspaceId, String title, Boolean isOwner) {
            this.workspaceId = workspaceId;
            this.title = title;
            this.isOwner = isOwner;
        }
    }
}
