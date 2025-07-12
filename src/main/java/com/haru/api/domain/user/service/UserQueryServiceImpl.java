package com.haru.api.domain.user.service;

import com.haru.api.domain.user.converter.UserConverter;
import com.haru.api.domain.user.dto.UserResponseDTO;
import com.haru.api.domain.user.entity.Users;
import com.haru.api.domain.user.repository.UserRepository;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.MemberHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository userRepository;

    @Override
    public UserResponseDTO.User getUserInfo(Long userId) {

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        return UserConverter.toUserDTO(user);
    }

    @Override
    public List<UserResponseDTO.User> getSimilarEmailUsers(Long userId, String email) {

        userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        List<Users> users = userRepository.findTop4UsersByEmailContainingIgnoreCase(email);

        return users.parallelStream()
                .map(user -> UserResponseDTO.User.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .imageUrl(user.getProfileImage())
                        .name(user.getName())
                        .build())
                .toList();
    }
}
