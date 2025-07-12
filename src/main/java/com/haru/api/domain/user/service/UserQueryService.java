package com.haru.api.domain.user.service;

import com.haru.api.domain.user.dto.UserResponseDTO;

import java.util.List;

public interface UserQueryService {
    UserResponseDTO.User getUserInfo(Long userId);

    List<UserResponseDTO.User> getSimilarEmailUsers(Long userId, String email);
}
