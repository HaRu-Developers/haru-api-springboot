package com.haru.api.domain.lastOpened.entity;

import com.haru.api.domain.lastOpened.entity.enums.DocumentType;

public interface Documentable {
    Long getId();
    String getTitle();
    Long getWorkspaceId();
    DocumentType getDocumentType();
}