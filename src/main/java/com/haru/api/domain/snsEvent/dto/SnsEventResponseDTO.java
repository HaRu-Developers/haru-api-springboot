package com.haru.api.domain.snsEvent.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

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

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetSnsEventListRequest {
        private List<SnsEventList> snsEventList;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SnsEventList {
        private Long snsEventId;
        private String title;
        private int participantCount;
        private int winnerCount;
        private String snsLink;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime updatedAt;
    }
}
