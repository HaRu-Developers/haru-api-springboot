package com.haru.api.domain.moodTracker.service;

public interface MoodTrackerMailService {
    void sendSurveyLinkToEmail(
            Long moodTrackerId,
            String mailTitle,
            String mailContent
    );
}
