package com.haru.api.domain.user.controller;

import com.haru.api.domain.user.dto.UserRequestDTO;
import com.haru.api.domain.user.dto.UserResponseDTO;
import com.haru.api.domain.user.service.UserCommandService;
import com.haru.api.domain.user.service.UserQueryService;
import com.haru.api.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    public ApiResponse<Object> login(
            @RequestBody @Valid UserRequestDTO.LoginRequest request
    ) {
        userCommandService.login(request);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "회원 정보 조회", description =
            "# 회원 정보 조회 API 입니다. 현재는 jwt token을 구현하지 않아 pathvariable로 userId를 넣어주세요.추후 jwt token이 구현되면 수정하겠습니다."
    )
    @GetMapping("/{userId}/info")
    public ApiResponse<UserResponseDTO.UserDTO> getUserInfo(
            @PathVariable Long userId
    ) {
        UserResponseDTO.UserDTO userDTO = userQueryService.getUserInfo(userId);

        return ApiResponse.onSuccess(userDTO);
    }

    @Operation(summary = "회원 정보 수정", description =
            "# 회원 정보 수정 API 입니다. 현재는 jwt token을 구현하지 않아 pathvariable로 userId를 넣어주세요.추후 jwt token이 구현되면 수정하겠습니다."
    )
    @PatchMapping("/{userId}/info")
    public ApiResponse<UserResponseDTO.UserDTO> updateUserInfo(
            @PathVariable Long userId,
            @RequestBody @Valid UserRequestDTO.UserInfoUpdateRequest request
    ) {
        UserResponseDTO.UserDTO userDTO = userCommandService.updateUserInfo(userId, request);

        return ApiResponse.onSuccess(userDTO);
    }
}
