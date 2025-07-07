package com.haru.api.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserRequestDTO {
    @Getter
    @Builder
    public static class SignUpRequest {
        @NotBlank(message = "이메일은 빈값일 수 없습니다.")
        private String email;
        @NotBlank(message = "비밀번호는 빈값일 수 없습니다.")
        private String password;
        @NotBlank(message = "이름은 빈값일 수 없습니다.")
        private String name;
    }
}
