package com.haru.api.domain.lastOpened.converter;

import com.haru.api.domain.lastOpened.entity.Documentable;
import com.haru.api.domain.lastOpened.entity.UserDocumentId;
import com.haru.api.domain.lastOpened.entity.UserDocumentLastOpened;
import com.haru.api.domain.user.entity.User;

public class UserDocumentLastOpenedConverter {
    public static UserDocumentLastOpened toUserDocumentLastOpened(Documentable document, User user) {

        UserDocumentId userDocumentId = new UserDocumentId(user.getId(), document.getId(), document.getDocumentType());

        return UserDocumentLastOpened.builder()
                .id(userDocumentId)
                .user(user)
                .workspaceId(document.getWorkspaceId())
                .lastOpened(null)
                .thumbnailKeyName(document.getThumbnailKeyName())
                .title(document.getTitle())
                .build();
    }
}
