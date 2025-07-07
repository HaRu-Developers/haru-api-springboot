package com.haru.api.domain.user.service;

import com.haru.api.domain.user.dto.UserRequestDTO;
import com.haru.api.domain.user.entity.Users;
import jakarta.validation.Valid;

public interface UserCommandService {
    void signUp(UserRequestDTO.SignUpRequest request);
}
