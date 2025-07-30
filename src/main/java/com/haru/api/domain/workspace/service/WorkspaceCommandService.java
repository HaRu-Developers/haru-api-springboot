package com.haru.api.domain.workspace.service;

import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.workspace.dto.WorkspaceRequestDTO;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;

public interface WorkspaceCommandService {

    WorkspaceResponseDTO.Workspace createWorkspace(Long userId, WorkspaceRequestDTO.WorkspaceCreateRequest request);

    WorkspaceResponseDTO.Workspace updateWorkspace(Long userId, Long workspaceid, WorkspaceRequestDTO.WorkspaceUpdateRequest request);

    WorkspaceResponseDTO.InvitationAcceptResult acceptInvite(String token);

    WorkspaceResponseDTO.InvitationAcceptResult acceptInvite(String token, User user);

    void sendInviteEmail(Long userId, WorkspaceRequestDTO.WorkspaceInviteEmailRequest request);
}
