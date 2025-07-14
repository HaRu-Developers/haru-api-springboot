package com.haru.api.domain.workspace.converter;

import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import com.haru.api.domain.workspace.entity.Workspace;

public class WorkspaceConverter {
    public static WorkspaceResponseDTO.Workspace toWorkspaceDTO(Workspace workspace) {
        return WorkspaceResponseDTO.Workspace.builder()
                .workspaceId(workspace.getId())
                .name(workspace.getTitle())
                .imageUrl(workspace.getImageUrl())
                .build();
    }
}
