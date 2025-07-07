package com.haru.api.domain.user.service;

import com.haru.api.domain.user.dto.UserResponseDTO;

public interface UserQueryService {
    UserResponseDTO.UserDTO getUserInfo(Long userId);
}
