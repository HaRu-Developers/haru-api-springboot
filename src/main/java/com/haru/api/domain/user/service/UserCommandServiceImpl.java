package com.haru.api.domain.user.service;

import com.haru.api.domain.user.converter.UserConverter;
import com.haru.api.domain.user.dto.UserRequestDTO;
import com.haru.api.domain.user.dto.UserResponseDTO;
import com.haru.api.domain.user.entity.Users;
import com.haru.api.domain.user.repository.UserRepository;
import com.haru.api.domain.user.security.jwt.JwtUtils;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.MemberHandler;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserCommandServiceImpl implements UserCommandService{

    @Value("${jwt.ACCESS_EXP_TIME}")
    private int accessExpTime;
    @Value("${jwt.REFRESH_EXP_TIME}")
    private int refreshExpTime;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtUtils jwtUtils;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void signUp(UserRequestDTO.SignUpRequest request) {
        Users user = UserConverter.toUsers(request);
        user.encodePassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
    }

    @Override
    public UserResponseDTO.LoginResponse login(UserRequestDTO.LoginRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // jwt토큰(access token, refresh token) 생성
        Users getUser = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));
        String key = "users:" + getUser.getId().toString();
        String accessToken = generateAccessToken(getUser.getId(), accessExpTime);
        String refreshToken = generateAndSaveRefreshToken(key, refreshExpTime);

        return UserConverter.toLoginResponse(getUser, accessToken, refreshToken);
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

    private String generateAccessToken(Long userId, int accessExpTime) {
        // 인증 완료 후 jwt토큰(accessToken) 생성
        Map<String, Object> valueMap = Map.of(
                "userId", userId
        );
        return jwtUtils.generateToken(valueMap, accessExpTime);
    }

    private String generateAndSaveRefreshToken(String key, int refreshExpTime) {
        // 인증 완료 후 jwt토큰(refreshToken) 생성
        String refreshToken = jwtUtils.generateToken(Collections.emptyMap(), refreshExpTime);
        redisTemplate.opsForValue().set(key, refreshToken, refreshExpTime, TimeUnit.MINUTES);
        return refreshToken;
    }
}
