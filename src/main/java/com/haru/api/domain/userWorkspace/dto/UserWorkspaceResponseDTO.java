package com.haru.api.domain.userWorkspace.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public class UserWorkspaceResponseDTO {

    @Getter
    @Builder
    public static class UserWorkspaceWithTitle {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long workspaceId;
        private String title;
        @Setter
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
