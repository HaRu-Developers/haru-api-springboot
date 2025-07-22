package com.haru.api.domain.workspace.service;

import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;

public interface WorkspaceQueryService {

    WorkspaceResponseDTO.Documents getDocuments(Long userId, Long workspaceId, String title);
}
