package com.haru.api.domain.user.converter;

import com.haru.api.domain.user.dto.UserRequestDTO;
import com.haru.api.domain.user.dto.UserResponseDTO;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.user.entity.enums.Status;

public class UserConverter {
    public static User toUsers(UserRequestDTO.SignUpRequest request, String password) {
        return User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .password(password)
                .marketingAgreed(request.isMarketingAgreed())
                .status(Status.ACTIVE)
                .build();
    }

    public static UserResponseDTO.User toUserDTO(User user) {
        return UserResponseDTO.User.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    public static UserResponseDTO.LoginResponse toLoginResponse(User user, String accessToken, String refreshToken) {
        return UserResponseDTO.LoginResponse.builder()
                .userid(user.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public static UserResponseDTO.RefreshResponse toRefreshResponse(Long userId, String accessToken, String refreshToken) {
        return UserResponseDTO.RefreshResponse.builder()
                .userId(userId)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public static UserResponseDTO.MemberInfo toMemberInfo(User user) {
        return UserResponseDTO.MemberInfo.builder()
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }
}
