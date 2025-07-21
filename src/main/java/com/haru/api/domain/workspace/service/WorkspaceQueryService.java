package com.haru.api.domain.workspace.service;

import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;

import java.util.List;

public interface WorkspaceQueryService {

    List<WorkspaceResponseDTO.Document> getDocuments(Long userId, Long workspaceId, String title);
}
