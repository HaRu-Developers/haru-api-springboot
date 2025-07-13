package com.haru.api.domain.user.controller;

import com.haru.api.domain.user.dto.UserRequestDTO;
import com.haru.api.domain.user.dto.UserResponseDTO;
import com.haru.api.domain.user.security.jwt.SecurityUtil;
import com.haru.api.domain.user.service.UserCommandService;
import com.haru.api.domain.user.service.UserQueryService;
import com.haru.api.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;

    @Operation(summary = "회원가입", description =
            "# 회원가입 API 입니다. 이메일과 패스워드 그리고 이름을 body에 입력해주세요."
    )
    @PostMapping("/signup")
    public ApiResponse<Object> signUp(
            @RequestBody @Valid UserRequestDTO.SignUpRequest request
    ) {
        userCommandService.signUp(request);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "로그인", description =
            "# 로그인 API 입니다. 이메일과 패스워드를 body에 입력해주세요."
    )
    @PostMapping("/login")
    public ApiResponse<UserResponseDTO.LoginResponse> login(
            @RequestBody @Valid UserRequestDTO.LoginRequest request
    ) {
        return ApiResponse.onSuccess(
                userCommandService.login(request)
        );
    }

    @Operation(summary = "토큰 갱신", description =
            "# access token 갱신 API 입니다. access token과 refresh token을 header에 입력해주세요."
    )
    @PostMapping("/refresh")
    public ApiResponse<UserResponseDTO.RefreshResponse> refreshToken(
            @RequestHeader("RefreshToken") String refreshToken
    ) {
        return ApiResponse.onSuccess(
                userCommandService.refresh(refreshToken)
        );
    }

    @Operation(summary = "로그아웃 API", description =
            "# 로그아웃 API 입니다. 로그아웃하고자 하는 유저의 access token을 header에 입력해주세요."
    )
    @DeleteMapping("/logout")
    public ApiResponse<?> logout(
            @RequestHeader("Authorization") String accessToken
    ) {
        userCommandService.logout(accessToken);
        return ApiResponse.onSuccess("");
    }

    @Operation(summary = "회원 정보 조회", description =
            "# 회원 정보 조회 API 입니다. \n" +
                    "현재는 jwt token을 구현하지 않아 pathvariable로 userId를 넣어주세요.추후 jwt token이 구현되면 수정하겠습니다."
    )
    @GetMapping("/info")
    public ApiResponse<UserResponseDTO.User> getUserInfo() {

        Long userId = SecurityUtil.getCurrentUserId();

        UserResponseDTO.User user = userQueryService.getUserInfo(userId);

        return ApiResponse.onSuccess(user);
    }

    @Operation(summary = "회원 정보 수정", description =
            "# 회원 정보 수정 API 입니다. \n" +
                    "현재는 jwt token을 구현하지 않아 pathvariable로 userId를 넣어주세요.추후 jwt token이 구현되면 수정하겠습니다."
    )
    @PatchMapping("/info")
    public ApiResponse<UserResponseDTO.User> updateUserInfo(
            @RequestBody @Valid UserRequestDTO.UserInfoUpdateRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        UserResponseDTO.User user = userCommandService.updateUserInfo(userId, request);

        return ApiResponse.onSuccess(user);
    }

    @Operation(summary = "이메일로 회원 리스트 조회", description =
            "# 유사한 이메일을 가진 회원을 최대 4명까지 조회하는 API 입니다. \n" +
                    "workspace에서 회원을 초대할 때 사용할 기능입니다. \n" +
                    "현재는 jwt token을 구현하지 않아 pathvariable로 userId를 넣어주세요.추후 jwt token이 구현되면 수정하겠습니다."
    )
    @GetMapping("/search")
    public ApiResponse<List<UserResponseDTO.User>> searchUsers(
            @RequestParam String email
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        List<UserResponseDTO.User> users = userQueryService.getSimilarEmailUsers(userId, email);

        return ApiResponse.onSuccess(users);
    }

}
