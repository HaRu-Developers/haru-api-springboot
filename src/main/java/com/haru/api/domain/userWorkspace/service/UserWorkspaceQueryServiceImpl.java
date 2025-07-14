package com.haru.api.domain.userWorkspace.service;

import com.haru.api.domain.userWorkspace.dto.UserWorkspaceResponseDTO;
import com.haru.api.domain.userWorkspace.repository.UserWorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserWorkspaceQueryServiceImpl implements UserWorkspaceQueryService {

    private final UserWorkspaceRepository userWorkspaceRepository;

    @Override
    public List<UserWorkspaceResponseDTO.UserWorkspaceWithTitle> getUserWorkspaceList(Long userId) {

        return userWorkspaceRepository.getUserWorkspacesWithTitle(userId);
    }
}
