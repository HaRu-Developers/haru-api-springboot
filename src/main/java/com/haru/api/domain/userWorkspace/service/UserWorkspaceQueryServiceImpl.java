package com.haru.api.domain.userWorkspace.service;

import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.userWorkspace.dto.UserWorkspaceResponseDTO;
import com.haru.api.domain.userWorkspace.repository.UserWorkspaceRepository;
import com.haru.api.infra.s3.AmazonS3Manager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserWorkspaceQueryServiceImpl implements UserWorkspaceQueryService {

    private final UserWorkspaceRepository userWorkspaceRepository;
    private final AmazonS3Manager amazonS3Manager;

    @Override
    public List<UserWorkspaceResponseDTO.UserWorkspaceWithTitle> getUserWorkspaceList(User user) {

        List<UserWorkspaceResponseDTO.UserWorkspaceWithTitle> workspaceList =
                userWorkspaceRepository.getUserWorkspacesWithTitle(user.getId());

        workspaceList.forEach(workspace -> {
            String presignedUrl = amazonS3Manager.generatePresignedUrl(workspace.getImageUrl());
            workspace.setImageUrl(presignedUrl);
        });

        return workspaceList;
    }
}
