package com.haru.api.domain.workspace.service;

import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.user.repository.UserRepository;
import com.haru.api.domain.userWorkspace.entity.enums.Auth;
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
import com.haru.api.global.apiPayload.exception.handler.UserWorkspaceHandler;
import com.haru.api.global.apiPayload.exception.handler.WorkspaceHandler;
import com.haru.api.global.apiPayload.exception.handler.WorkspaceInvitationHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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

        UserWorkspace userWorkspace = userWorkspaceRepository.findByWorkspaceIdAndUserId(foundWorkspace.getId(), foundUser.getId())
                .orElseThrow(() -> new UserWorkspaceHandler(ErrorStatus.USER_WORKSPACE_NOT_FOUND));

        if(userWorkspace.getAuth() != Auth.ADMIN)
            throw new WorkspaceHandler(ErrorStatus.WORKSPACE_MODIFY_NOT_ALLOWED);

        foundWorkspace.updateTitle(request.getTitle());

        return WorkspaceConverter.toWorkspaceDTO(foundWorkspace);
    }

    @Transactional
    @Override
    public WorkspaceResponseDTO.InvitationAcceptResult acceptInvite(String token) {

        WorkspaceInvitation foundWorkspaceInvitation = workspaceInvitationRepository.findByToken(token)
                .orElseThrow(() -> new WorkspaceInvitationHandler(ErrorStatus.INVITATION_NOT_FOUND));

        Workspace foundWorkspace = workspaceRepository.findById(foundWorkspaceInvitation.getWorkspace().getId())
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

        // 이미 수락된 초대장이면 예외 발생
        if(foundWorkspaceInvitation.isAccepted())
            throw new WorkspaceInvitationHandler(ErrorStatus.ALREADY_ACCEPTED);

        // 초대받은 이메일로 가입된 사용자가 있는지 확인
        Optional<User> foundUser = userRepository.findByEmail(foundWorkspaceInvitation.getEmail());

        boolean isAlreadyRegistered = foundUser.isPresent();

        // 이미 가입된 사용자
        if(isAlreadyRegistered) {
            // 초대장을 수락했다고 db에 저장
            foundWorkspaceInvitation.setAccepted();
        } else {
            // 가입되지 않은 사용자면 not success
            return WorkspaceConverter.toInvitationAcceptResult(false, false, foundWorkspace);
        }

        // 가입된 사용자인 경우 워크스페이스에 추가
        userWorkspaceRepository.save(UserWorkspace.builder()
                .workspace(foundWorkspace)
                .user(foundUser.get())
                .auth(Auth.MEMBER)
                .build());

        return WorkspaceConverter.toInvitationAcceptResult(true, true, foundWorkspace);
    }

}
