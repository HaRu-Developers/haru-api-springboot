package com.haru.api.domain.user.dto;

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
        private String userId;
        private String accessToken;
        private String refreshToken;
    }

    @Getter
    @Builder
    public static class RefreshResponse {
        private String userId;
        private String accessToken;
        private String refreshToken;
    }
}
