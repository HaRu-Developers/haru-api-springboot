package com.haru.api.domain.user.service;

import com.haru.api.domain.user.converter.UserConverter;
import com.haru.api.domain.user.dto.UserResponseDTO;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository userRepository;

    @Override
    public UserResponseDTO.User getUserInfo(User user) {

        return UserConverter.toUserDTO(user);
    }

    @Override
    public List<UserResponseDTO.User> getSimilarEmailUsers(User user, String email) {

        List<User> users = userRepository.findTop4UsersByEmailContainingIgnoreCase(email);

        return users.parallelStream()
                .map(eachUser -> UserResponseDTO.User.builder()
                        .id(eachUser.getId())
                        .email(eachUser.getEmail())
                        .imageUrl(eachUser.getProfileImage())
                        .name(eachUser.getName())
                        .build())
                .toList();
    }
}
