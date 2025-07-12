package com.haru.api.domain.workspace.service;

import com.haru.api.domain.workspace.dto.WorkspaceRequestDTO;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;

public interface WorkspaceCommandService {

    WorkspaceResponseDTO.Workspace createWorkspace(Long userId, WorkspaceRequestDTO.WorkspaceCreateRequest request);
}
