package com.haru.api.domain.workspace.service;

import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.workspace.dto.WorkspaceRequestDTO;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import com.haru.api.domain.workspace.entity.Workspace;
import org.springframework.web.multipart.MultipartFile;

public interface WorkspaceCommandService {

    WorkspaceResponseDTO.Workspace createWorkspace(User user, WorkspaceRequestDTO.WorkspaceCreateRequest request, MultipartFile image);

    WorkspaceResponseDTO.Workspace updateWorkspace(User user, Workspace workspace, WorkspaceRequestDTO.WorkspaceUpdateRequest request, MultipartFile image);

    WorkspaceResponseDTO.InvitationAcceptResult acceptInvite(String token);

    WorkspaceResponseDTO.InvitationAcceptResult acceptInvite(String token, User user);

    void sendInviteEmail(User user, WorkspaceRequestDTO.WorkspaceInviteEmailRequest request);
}
