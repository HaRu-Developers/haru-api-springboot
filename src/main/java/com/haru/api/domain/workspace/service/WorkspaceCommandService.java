package com.haru.api.domain.workspace.service;

import com.haru.api.domain.workspace.dto.WorkspaceRequestDTO;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;

public interface WorkspaceCommandService {

    WorkspaceResponseDTO.Workspace createWorkspace(Long userId, WorkspaceRequestDTO.WorkspaceCreateRequest request);

    WorkspaceResponseDTO.Workspace updateWorkspace(Long userId, Long workspaceid, WorkspaceRequestDTO.WorkspaceUpdateRequest request);

    void acceptInvite(String code);

    void sendInviteEmail(Long userId, WorkspaceRequestDTO.WorkspaceInviteEmailRequest request);
}
