package com.haru.api.domain.userWorkspace.dto;

import lombok.Builder;
import lombok.Getter;

public class UserWorkspaceResponseDTO {

    @Getter
    @Builder
    public static class UserWorkspaceWithTitle {
        private Long workspaceId;
        private String title;
        private String imageUrl;
        private Boolean isOwner;

        public UserWorkspaceWithTitle(Long workspaceId, String title, String imageUrl, Boolean isOwner) {
            this.workspaceId = workspaceId;
            this.title = title;
            this.imageUrl = imageUrl;
            this.isOwner = isOwner;
        }
    }
}
