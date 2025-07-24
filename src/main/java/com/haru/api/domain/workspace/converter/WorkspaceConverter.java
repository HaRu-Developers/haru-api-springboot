package com.haru.api.domain.workspace.converter;

import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import com.haru.api.domain.workspace.entity.Workspace;

import java.util.List;

public class WorkspaceConverter {
    public static WorkspaceResponseDTO.Workspace toWorkspaceDTO(Workspace workspace) {
        return WorkspaceResponseDTO.Workspace.builder()
                .workspaceId(workspace.getId())
                .name(workspace.getTitle())
                .imageUrl(workspace.getImageUrl())
                .build();
    }

    public static WorkspaceResponseDTO.Documents toDocumentsDTO(List<WorkspaceResponseDTO.Document> documentList) {
        return WorkspaceResponseDTO.Documents.builder()
                .documentList(documentList)
                .build();
    }
}
