package com.haru.api.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

public class UserResponseDTO {

    @Getter
    @Builder
    public static class UserDTO {
        private Long id;
        private String email;
        private String imageUrl;
        private String name;
    }
}
