package com.haru.api.domain.snsEvent.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public class SnsEventResponseDTO {
    @Getter
    @Builder
    public static class CreateSnsEventResponse {
        private Long userId;
        private String accessToken;
        private String refreshToken;
    }

    @Getter
    @Builder
    public static class LinkInstagramAccountResponse {
        private String instagramAccountName;
    }

    @Getter
    public static class InstagramMediaResponse {
        private List<Media> data;
    }

    @Getter
    public static class Media {
        private String shortcode;
        private String id;
    }

    @Getter
    public static class InstagramCommentResponse {
        private List<Comment> data;
    }

    @Getter
    public static class Comment {
        private From from;
        private String text;
        private OffsetDateTime timestamp;
        private String id;
    }

    @Getter
    public static class From {
        private String id;
        private String username;
    }
}
