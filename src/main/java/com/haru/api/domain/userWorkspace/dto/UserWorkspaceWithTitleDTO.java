package com.haru.api.domain.userWorkspace.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserWorkspaceWithTitleDTO {
    private Long workspaceId;
    private String title;
    private Boolean isOwner;

    public UserWorkspaceWithTitleDTO(Long workspaceId, String title, Boolean isOwner) {
        this.workspaceId = workspaceId;
        this.title = title;
        this.isOwner = isOwner;
    }
}
