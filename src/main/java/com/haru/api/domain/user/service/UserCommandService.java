package com.haru.api.domain.user.service;

import com.haru.api.domain.user.dto.UserRequestDTO;
import com.haru.api.domain.user.dto.UserResponseDTO;

public interface UserCommandService {
    void signUp(UserRequestDTO.SignUpRequest request);
    UserResponseDTO.LoginResponse login(UserRequestDTO.LoginRequest request);

    UserResponseDTO.User updateUserInfo(Long userId, UserRequestDTO.UserInfoUpdateRequest request);

    UserResponseDTO.RefreshResponse refresh(String refreshToken);

    void logout(String accessToken);
}
