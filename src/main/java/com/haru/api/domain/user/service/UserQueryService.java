package com.haru.api.domain.user.service;

import com.haru.api.domain.user.dto.UserResponseDTO;
import com.haru.api.domain.user.entity.User;

import java.util.List;

public interface UserQueryService {
    UserResponseDTO.User getUserInfo(User user);

    List<UserResponseDTO.User> getSimilarEmailUsers(User user, String email);
}
