package com.haru.api.domain.user.controller;

import com.haru.api.domain.user.dto.UserRequestDTO;
import com.haru.api.domain.user.dto.UserResponseDTO;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.user.service.UserCommandService;
import com.haru.api.domain.user.service.UserQueryService;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import com.haru.api.domain.workspace.service.WorkspaceCommandService;
import com.haru.api.global.annotation.AuthUser;
import com.haru.api.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    private final WorkspaceCommandService workspaceCommandService;

    @Operation(summary = "회원가입 [v1.0 (2025-08-05)]", description =
            "# [v1.0 (2025-08-05)](https://www.notion.so/2265da7802c580e8b883e3e4481fd61d?v=2265da7802c5816ab095000cc1ddadca&p=2265da7802c5819ca025d31fe9167842&pm=s)" +
                    " 회원가입 API 입니다. 이메일과 패스워드 그리고 이름을 body에 입력해주세요. 워크스페이스 초대 메일을 통한 회원가입은 query string에 초대장의 token을 넣어주세요"
    )
    @PostMapping("/signup")
    public ApiResponse<Object> signUp(
            @RequestBody @Valid UserRequestDTO.SignUpRequest request,
            @RequestParam(required = false) String token
    ) {
        User user = userCommandService.signUp(request);

        // 워크스페이스 초대 메일을 통하여 회원가입한 경우
        if (token != null) {
            WorkspaceResponseDTO.InvitationAcceptResult invitationAcceptResult = workspaceCommandService.acceptInvite(token, user);
            return ApiResponse.onSuccess(invitationAcceptResult);
        }

        // 일반 회원가입
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "로그인 [v1.0 (2025-08-05)]", description =
            "# [v1.0 (2025-08-05)](https://www.notion.so/2265da7802c580e8b883e3e4481fd61d?v=2265da7802c5816ab095000cc1ddadca&p=2265da7802c58194963acb7f32017fc8&pm=s)" +
                    " 로그인 API 입니다. 이메일과 패스워드를 body에 입력해주세요."
    )
    @PostMapping("/login")
    public ApiResponse<UserResponseDTO.LoginResponse> login(
            @RequestBody @Valid UserRequestDTO.LoginRequest request
    ) {
        return ApiResponse.onSuccess(
                userCommandService.login(request)
        );
    }

    @Operation(summary = "Access Token 갱신 [v1.0 (2025-08-05)]", description =
            "# [v1.0 (2025-08-05)](https://www.notion.so/2265da7802c580e8b883e3e4481fd61d?v=2265da7802c5816ab095000cc1ddadca&p=2265da7802c581939506e2e295eb29a0&pm=s)" +
                    " Access Token 갱신 API 입니다. Access Token과 Refresh Token을 header에 입력해주세요."
    )
    @PostMapping("/refresh")
    public ApiResponse<UserResponseDTO.RefreshResponse> refreshToken(
            @RequestHeader("RefreshToken") String refreshToken
    ) {
        return ApiResponse.onSuccess(
                userCommandService.refresh(refreshToken)
        );
    }

    @Operation(summary = "로그아웃 API [v1.0 (2025-08-05)]", description =
            "# [v1.0 (2025-08-05)](https://www.notion.so/2265da7802c580e8b883e3e4481fd61d?v=2265da7802c5816ab095000cc1ddadca&p=2265da7802c581da88d1faf700b112de&pm=s)" +
                    "로그아웃 API 입니다. 로그아웃하고자 하는 유저의 Access Token을 header에 입력해주세요."
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
    public ApiResponse<UserResponseDTO.User> getUserInfo(
            @Parameter(hidden = true) @AuthUser User user
    ) {
        UserResponseDTO.User userResponse = userQueryService.getUserInfo(user);

        return ApiResponse.onSuccess(userResponse);
    }

    @Operation(summary = "회원 정보 수정", description =
            "# 회원 정보 수정 API 입니다. \n" +
                    "현재는 jwt token을 구현하지 않아 pathvariable로 userId를 넣어주세요.추후 jwt token이 구현되면 수정하겠습니다."
    )
    @PatchMapping("/info")
    public ApiResponse<UserResponseDTO.User> updateUserInfo(
            @RequestBody @Valid UserRequestDTO.UserInfoUpdateRequest request,
            @Parameter(hidden = true) @AuthUser User user
    ) {
        UserResponseDTO.User userResponse = userCommandService.updateUserInfo(user, request);

        return ApiResponse.onSuccess(userResponse);
    }

    @Operation(summary = "이메일로 회원 리스트 조회", description =
            "# 유사한 이메일을 가진 회원을 최대 4명까지 조회하는 API 입니다. \n" +
                    "workspace에서 회원을 초대할 때 사용할 기능입니다. \n" +
                    "현재는 jwt token을 구현하지 않아 pathvariable로 userId를 넣어주세요.추후 jwt token이 구현되면 수정하겠습니다."
    )
    @GetMapping("/search")
    public ApiResponse<List<UserResponseDTO.User>> searchUsers(
            @RequestParam String email,
            @Parameter(hidden = true) @AuthUser User user
    ) {
        List<UserResponseDTO.User> users = userQueryService.getSimilarEmailUsers(user, email);

        return ApiResponse.onSuccess(users);
    }

    @Operation(summary = "이메일 중복 검사 [v1.0 (2025-08-05)]", description =
            "# [v1.0 (2025-08-05)](https://www.notion.so/API-21e5da7802c581cca23dff937ac3f155?p=22a5da7802c580c0b553c6223d3efe53&pm=s)" +
                    " 회원가입 시 이미 가입된 회원의 이메일인지 중복 검사하는 API입니다."
    )
    @PostMapping("/signup/same")
    public ApiResponse<UserResponseDTO.CheckEmailDuplicationResponse> checkEmailDuplication(
            @RequestBody UserRequestDTO.CheckEmailDuplicationRequest request
    ) {
        return ApiResponse.onSuccess(
                userCommandService.checkEmailDuplication(
                        request
                )
        );
    }

    @Operation(summary = "기존 비밀번호 일치 검사 [v1.0 (2025-08-12)]", description =
            "# [v1.0 (2025-08-12)](https://www.notion.so/24d5da7802c580c68535f3a5982d82d2)" +
                    " 비밀번호 변경 시 기존 비밀버호가 일치하는지 확인하는 API입니다."
    )
    @PostMapping("/password/check")
    public ApiResponse<UserResponseDTO.CheckOriginalPasswordResponse> checkOriginalPassword(
            @RequestBody UserRequestDTO.CheckOriginalPasswordRequest request,
            @Parameter(hidden = true) @AuthUser User user
    ) {
        return ApiResponse.onSuccess(userCommandService.checkOriginalPassword(request, user));
    }
}
