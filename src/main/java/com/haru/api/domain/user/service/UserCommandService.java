package com.haru.api.domain.user.service;

import com.haru.api.domain.user.dto.UserRequestDTO;
import com.haru.api.domain.user.dto.UserResponseDTO;

public interface UserCommandService {
    void signUp(UserRequestDTO.SignUpRequest request);

    UserResponseDTO.UserDTO updateUserInfo(Long userId, UserRequestDTO.UserInfoUpdateRequest request);
}
