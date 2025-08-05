package com.haru.api.domain.user.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Getter;

public class UserResponseDTO {

    @Getter
    @Builder
    public static class User {
        private Long id;
        private String email;
        private String imageUrl;
        private String name;
    }

    @Getter
    @Builder
    public static class LoginResponse {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long userId;
        private String accessToken;
        private String refreshToken;
    }

    @Getter
    @Builder
    public static class RefreshResponse {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long userId;
        private String accessToken;
        private String refreshToken;
    }
}
