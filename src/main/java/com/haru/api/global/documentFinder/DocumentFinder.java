package com.haru.api.global.documentFinder;

import com.haru.api.domain.lastOpened.entity.enums.DocumentType;

public interface DocumentFinder {
    DocumentType getSupportType();
    Object findById(Object id);
}
