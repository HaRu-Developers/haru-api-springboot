package com.haru.api.domain.user.service;

import com.haru.api.domain.user.dto.UserResponseDTO;

import java.util.List;

public interface UserQueryService {
    UserResponseDTO.UserDTO getUserInfo(Long userId);

    List<UserResponseDTO.UserDTO> getSimilarEmailUsers(Long userId, String email);
}
