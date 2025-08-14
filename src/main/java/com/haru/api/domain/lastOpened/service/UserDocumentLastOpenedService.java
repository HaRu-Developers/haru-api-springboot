package com.haru.api.domain.lastOpened.service;

import com.haru.api.domain.lastOpened.entity.Documentable;
import com.haru.api.domain.lastOpened.entity.enums.DocumentType;
import com.haru.api.domain.user.entity.User;

import java.util.List;

public interface UserDocumentLastOpenedService {

    void updateLastOpened(Long userId, DocumentType documentType, Long documentId, Long workspaceId, String title);

    void createInitialRecordsForWorkspaceUsers(List<User> usersInWorkspace, Documentable document);

    void deleteRecordsForWorkspaceUsers(Documentable document);

    void updateRecordsForWorkspaceUsers(Documentable document);
}
