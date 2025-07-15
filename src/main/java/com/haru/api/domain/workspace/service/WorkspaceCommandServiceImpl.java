package com.haru.api.domain.workspace.service;

import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.user.repository.UserRepository;
import com.haru.api.domain.userWorkspace.entity.Auth;
import com.haru.api.domain.userWorkspace.entity.UserWorkspace;
import com.haru.api.domain.userWorkspace.repository.UserWorkspaceRepository;
import com.haru.api.domain.workspace.converter.WorkspaceConverter;
import com.haru.api.domain.workspace.dto.WorkspaceRequestDTO;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.domain.workspace.repository.WorkspaceRepository;
import com.haru.api.domain.workspaceInvitation.entity.WorkspaceInvitation;
import com.haru.api.domain.workspaceInvitation.repository.WorkspaceInvitationRepository;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.MemberHandler;
import com.haru.api.global.apiPayload.exception.handler.WorkspaceHandler;
import com.haru.api.global.apiPayload.exception.handler.WorkspaceInvitationHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceCommandServiceImpl implements WorkspaceCommandService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserWorkspaceRepository userWorkspaceRepository;
    private final WorkspaceInvitationRepository workspaceInvitationRepository;

    @Transactional
    @Override
    public WorkspaceResponseDTO.Workspace createWorkspace(Long userId, WorkspaceRequestDTO.WorkspaceCreateRequest request) {

        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        // workspace 생성 및 저장
        Workspace workspace = workspaceRepository.save(Workspace.builder()
                .title(request.getName())
                        .creator(foundUser)
                .build());


        // s3에 사진 추가하는 메서드

        // request로 받은 이메일로 초대 메일 전송하는 메서드

        // users_workspaces 테이블에 생성자 정보 저장
        userWorkspaceRepository.save(UserWorkspace.builder()
                .user(foundUser)
                .workspace(workspace)
                .auth(Auth.ADMIN)
                .build());

        return WorkspaceConverter.toWorkspaceDTO(workspaceRepository.save(workspace));
    }

    @Transactional
    @Override
    public WorkspaceResponseDTO.Workspace updateWorkspace(Long userId, Long workspaceId, WorkspaceRequestDTO.WorkspaceUpdateRequest request) {

        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        Workspace foundWorkspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

        if (foundUser.getId() != foundWorkspace.getCreator().getId())
            throw new WorkspaceHandler(ErrorStatus.WORKSPACE_MODIFY_NOT_ALLOWED);

        foundWorkspace.setTitle(request.getTitle());

        return WorkspaceConverter.toWorkspaceDTO(foundWorkspace);
    }

    @Transactional
    @Override
    public void acceptInvite(Long userId, String code) {

        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        WorkspaceInvitation foundWorkspaceInvitation = workspaceInvitationRepository.findByInvitationCode(code)
                .orElseThrow(() -> new WorkspaceInvitationHandler(ErrorStatus.INVITATION_NOT_FOUND));

        if(foundWorkspaceInvitation.getIsAccepted())
            throw new WorkspaceInvitationHandler(ErrorStatus.ALREADY_ACCEPTED);

        if(!foundWorkspaceInvitation.getEmail().equals(foundUser.getEmail()))
            throw new WorkspaceInvitationHandler(ErrorStatus.EMAIL_MISMATCH);

        userWorkspaceRepository.save(UserWorkspace.builder()
                .user(foundUser)
                .workspace(foundWorkspaceInvitation.getWorkspace())
                .auth(Auth.MEMBER)
                .build());

        foundWorkspaceInvitation.setIsAccepted(true);
    }


}
