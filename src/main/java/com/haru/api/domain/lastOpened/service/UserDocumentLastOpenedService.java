package com.haru.api.domain.lastOpened.service;

import com.haru.api.domain.lastOpened.entity.enums.DocumentType;

public interface UserDocumentLastOpenedService {

    void updateLastOpened(Long userId, DocumentType documentType, Long documentId, Long workspaceId, String title);
}
