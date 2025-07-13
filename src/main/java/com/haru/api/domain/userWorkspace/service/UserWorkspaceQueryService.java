package com.haru.api.domain.userWorkspace.service;

import com.haru.api.domain.userWorkspace.dto.UserWorkspaceWithTitleDTO;

import java.util.List;

public interface UserWorkspaceQueryService {

    List<UserWorkspaceWithTitleDTO> getUserWorkspaceList(Long userId);
}
