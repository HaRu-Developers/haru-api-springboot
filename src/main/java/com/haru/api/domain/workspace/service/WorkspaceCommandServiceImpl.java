package com.haru.api.domain.workspace.service;

import com.haru.api.domain.user.entity.Users;
import com.haru.api.domain.user.repository.UserRepository;
import com.haru.api.domain.userWorkspace.entity.Auth;
import com.haru.api.domain.userWorkspace.entity.UserWorkspace;
import com.haru.api.domain.userWorkspace.repository.UserWorkspaceRepository;
import com.haru.api.domain.workspace.converter.WorkspaceConverter;
import com.haru.api.domain.workspace.dto.WorkspaceRequestDTO;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.domain.workspace.repository.WorkspaceRepository;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.MemberHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkspaceCommandServiceImpl implements WorkspaceCommandService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserWorkspaceRepository userWorkspaceRepository;

    @Override
    public WorkspaceResponseDTO.Workspace createWorkspace(Long userId, WorkspaceRequestDTO.WorkspaceCreateRequest request) {

        Users foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        // workspace 생성 및 저장
        Workspace workspace = workspaceRepository.save(com.haru.api.domain.workspace.entity.Workspace.builder()
                .title(request.getName())
                .build());


        // s3에 사진 추가하는 메서드

        // request로 받은 이메일로 초대 메일 전송하는 메서드

        // user_workspace 테이블에 생성자 정보 저장
        userWorkspaceRepository.save(UserWorkspace.builder()
                .user(foundUser)
                .workspace(workspace)
                .auth(Auth.ADMIN)
                .build());

        return WorkspaceConverter.toWorkspaceDTO(workspaceRepository.save(workspace));
    }
}
