package com.haru.api.domain.user.service;

import com.haru.api.domain.user.converter.UserConverter;
import com.haru.api.domain.user.dto.UserRequestDTO;
import com.haru.api.domain.user.dto.UserResponseDTO;
import com.haru.api.domain.user.entity.Users;
import com.haru.api.domain.user.repository.UserRepository;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.MemberHandler;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCommandServiceImpl implements UserCommandService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    @Override
    public void signUp(UserRequestDTO.SignUpRequest request) {
        Users user = UserConverter.toUsers(request);
        user.encodePassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
    }

    @Override
    public void login(UserRequestDTO.LoginRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
    }

    @Override
    @Transactional
    public UserResponseDTO.UserDTO updateUserInfo(Long userId, UserRequestDTO.UserInfoUpdateRequest request) {
        String name = request.getName();
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));
        user.setName(name);

        return UserConverter.toUserDTO(user);
    }
}
