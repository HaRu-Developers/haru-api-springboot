package com.haru.api.domain.lastOpened.converter;

import com.haru.api.domain.lastOpened.entity.UserDocumentId;
import com.haru.api.domain.lastOpened.entity.UserDocumentLastOpened;
import com.haru.api.domain.lastOpened.entity.enums.DocumentType;
import com.haru.api.domain.meeting.entity.Meeting;
import com.haru.api.domain.moodTracker.entity.MoodTracker;
import com.haru.api.domain.snsEvent.entity.SnsEvent;
import com.haru.api.domain.user.entity.User;

public class UserDocumentLastOpenedConverter {
    public static UserDocumentLastOpened toUserDocumentLastOpened(Meeting meeting, User user) {

        UserDocumentId userDocumentId = new UserDocumentId(user.getId(), meeting.getId(), DocumentType.AI_MEETING_MANAGER);

        return UserDocumentLastOpened.builder()
                .id(userDocumentId)
                .user(user)
                .lastOpened(null)
                .build();
    }

    public static UserDocumentLastOpened toUserDocumentLastOpened(SnsEvent snsEvent, User user) {

        UserDocumentId userDocumentId = new UserDocumentId(user.getId(), snsEvent.getId(), DocumentType.SNS_EVENT_ASSISTANT);

        return UserDocumentLastOpened.builder()
                .id(userDocumentId)
                .user(user)
                .lastOpened(null)
                .build();
    }

    public static UserDocumentLastOpened toUserDocumentLastOpened(MoodTracker moodTracker, User user) {

        UserDocumentId userDocumentId = new UserDocumentId(user.getId(), moodTracker.getId(), DocumentType.TEAM_MOOD_TRACKER);

        return UserDocumentLastOpened.builder()
                .id(userDocumentId)
                .user(user)
                .lastOpened(null)
                .build();
    }
}
