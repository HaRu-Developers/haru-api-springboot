package com.haru.api.domain.user.service;

import com.haru.api.domain.user.dto.UserRequestDTO;
import com.haru.api.domain.user.dto.UserResponseDTO;
import com.haru.api.domain.user.entity.User;

public interface UserCommandService {
    User signUp(UserRequestDTO.SignUpRequest request);
    UserResponseDTO.LoginResponse login(UserRequestDTO.LoginRequest request);

    UserResponseDTO.User updateUserInfo(User user, UserRequestDTO.UserInfoUpdateRequest request);

    UserResponseDTO.RefreshResponse refresh(String refreshToken);

    void logout(String accessToken);

    String generateAccessToken(Long userId, int accessExpTime);

    String generateAndSaveRefreshToken(String key, int refreshExpTime);

    UserResponseDTO.CheckEmailDuplicationResponse checkEmailDuplication(UserRequestDTO.CheckEmailDuplicationRequest request);

    UserResponseDTO.CheckOriginalPasswordResponse checkOriginalPassword(UserRequestDTO.CheckOriginalPasswordRequest request, User user);

    UserResponseDTO.LoginResponse signupAndLoginAndInviteAccept(UserRequestDTO.SignUpRequest request, String token);
}
