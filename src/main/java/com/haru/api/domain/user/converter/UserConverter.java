package com.haru.api.domain.user.converter;

import com.haru.api.domain.user.dto.UserRequestDTO;
import com.haru.api.domain.user.dto.UserResponseDTO;
import com.haru.api.domain.user.entity.Users;
import com.haru.api.domain.user.entity.enums.Status;

public class UserConverter {
    public static Users toUsers(UserRequestDTO.SignUpRequest request) {
        return Users.builder()
                .email(request.getEmail())
                .name(request.getName())
                .status(Status.ACTIVE)
                .build();
    }

    public static UserResponseDTO.UserDTO toUserDTO(Users users) {
        return UserResponseDTO.UserDTO.builder()
                .id(users.getId())
                .email(users.getEmail())
                .name(users.getName())
                .build();
    }
}
