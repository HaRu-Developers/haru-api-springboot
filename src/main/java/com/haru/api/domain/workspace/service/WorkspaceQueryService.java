package com.haru.api.domain.workspace.service;

import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;

public interface WorkspaceQueryService {

    WorkspaceResponseDTO.DocumentList getDocuments(Long userId, Long workspaceId, String title);

    WorkspaceResponseDTO.DocumentWithoutLastOpenedList getDocumentWithoutLastOpenedList(Long userId, Long workspaceId);
}
