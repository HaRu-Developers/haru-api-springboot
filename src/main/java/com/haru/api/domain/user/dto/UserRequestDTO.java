package com.haru.api.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserRequestDTO {
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SignUpRequest {
        @NotBlank(message = "이메일은 빈값일 수 없습니다.")
        private String email;
        @NotBlank(message = "비밀번호는 빈값일 수 없습니다.")
        private String password;
        @NotBlank(message = "이름은 빈값일 수 없습니다.")
        private String name;
        private boolean marketingAgreed;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "이메일은 빈값일 수 없습니다.")
        private String email;
        @NotBlank(message = "비밀번호는 빈값일 수 없습니다.")
        private String password;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserInfoUpdateRequest {
        private String name;
        private String password;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CheckEmailDuplicationRequest {
        @NotBlank(message = "이메일은 빈값일 수 없습니다.")
        private String email;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CheckOriginalPasswordRequest {
        @NotBlank(message = "비밀번호는 빈값일 수 없습니다.")
        private String requestPassword;
    }
}
