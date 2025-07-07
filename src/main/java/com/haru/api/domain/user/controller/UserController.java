package com.haru.api.domain.user.controller;

import com.haru.api.domain.user.converter.UserConverter;
import com.haru.api.domain.user.dto.UserRequestDTO;
import com.haru.api.domain.user.service.UserCommandService;
import com.haru.api.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserCommandService userCommandService;

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
}
