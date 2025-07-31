package com.haru.api.domain.workspace.service;

import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.workspace.dto.WorkspaceRequestDTO;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface WorkspaceCommandService {

    WorkspaceResponseDTO.Workspace createWorkspace(Long userId, WorkspaceRequestDTO.WorkspaceCreateRequest request, MultipartFile image);

    WorkspaceResponseDTO.Workspace updateWorkspace(Long userId, Long workspaceId, WorkspaceRequestDTO.WorkspaceUpdateRequest request, MultipartFile image);

    WorkspaceResponseDTO.InvitationAcceptResult acceptInvite(String token);

    WorkspaceResponseDTO.InvitationAcceptResult acceptInvite(String token, User user);

    void sendInviteEmail(Long userId, WorkspaceRequestDTO.WorkspaceInviteEmailRequest request);
}
