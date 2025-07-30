package com.haru.api.domain.user.service;

import com.haru.api.domain.user.converter.UserConverter;
import com.haru.api.domain.user.dto.UserRequestDTO;
import com.haru.api.domain.user.dto.UserResponseDTO;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.user.repository.UserRepository;
import com.haru.api.domain.user.security.jwt.JwtUtils;
import com.haru.api.domain.user.security.jwt.SecurityUtil;
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

import static com.haru.api.global.apiPayload.code.status.ErrorStatus.REFRESH_TOKEN_NOT_EQUAL;

@Service
@RequiredArgsConstructor
public class UserCommandServiceImpl implements UserCommandService{

    @Value("${jwt.access-expiration}")
    private int accessExpTime;
    @Value("${jwt.refresh-expiration}")
    private int refreshExpTime;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtUtils jwtUtils;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void signUp(UserRequestDTO.SignUpRequest request) {
        String password = passwordEncoder.encode(request.getPassword());
        // 이메일 중복 확인
        User foundUser = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (foundUser != null) {
            throw new MemberHandler(ErrorStatus.MEMBER_ALREADY_EXISTS);
        } else {
            User user = UserConverter.toUsers(request, password);
            userRepository.save(user);
        }
    }

    @Override
    public UserResponseDTO.LoginResponse login(UserRequestDTO.LoginRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // jwt토큰(access token, refresh token) 생성
        User getUser = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));
        String key = "users:" + getUser.getId().toString();
        String accessToken = generateAccessToken(getUser.getId(), accessExpTime);
        String refreshToken = generateAndSaveRefreshToken(key, refreshExpTime);

        return UserConverter.toLoginResponse(getUser, accessToken, refreshToken);
    }

    @Override
    public UserResponseDTO.RefreshResponse refresh(String refreshToken) {
        Long userId = SecurityUtil.getCurrentUserId();
        String key = "users:" + userId.toString();
        String accessToken;
        String newRefreshToken;

        // 전달된 refresh token과 redis의 refresh token비교
        String getRefreshTokenFromRedis = redisTemplate.opsForValue().get(key);
        System.out.println("userId: " + userId);
        System.out.println("redis에서 가져온 refreshToken: " + getRefreshTokenFromRedis);
        if (refreshToken.equals(getRefreshTokenFromRedis)) {
            accessToken = generateAccessToken(userId, accessExpTime);
            newRefreshToken = generateAndSaveRefreshToken(key, refreshExpTime);
        } else {
            throw new MemberHandler(REFRESH_TOKEN_NOT_EQUAL);
        }

        return UserConverter.toRefreshResponse(userId, accessToken, newRefreshToken);
    }

    @Override
    public void logout(String accessToken) {
        // 로그아웃시킬 회원의 refresh token redis에서 삭제
        Long userId = SecurityUtil.getCurrentUserId();
        String key = "users:" + userId.toString();
        redisTemplate.delete(key);

        // 로그아웃시킬 회원의 access token redis의 블랙리스트로 저장, 인가 처리시 블랙리스트 확인을 통해 로그아웃된 회원인지 확인함.
        key = "blackList:" + userId.toString();
        long tokenRemainTimeSecond = jwtUtils.tokenRemainTimeSecond(accessToken);
        redisTemplate.opsForValue().set(key, accessToken, tokenRemainTimeSecond, TimeUnit.SECONDS);
    }


    @Transactional
    @Override
    public UserResponseDTO.User updateUserInfo(Long userId, UserRequestDTO.UserInfoUpdateRequest request) {
        String name = request.getName();

        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        foundUser.updateName(name);

        return UserConverter.toUserDTO(foundUser);
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
        redisTemplate.opsForValue().set(key, refreshToken, refreshExpTime, TimeUnit.SECONDS);
        return refreshToken;
    }
}
