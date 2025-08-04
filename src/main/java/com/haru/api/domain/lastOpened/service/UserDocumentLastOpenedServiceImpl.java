package com.haru.api.domain.lastOpened.service;

import com.haru.api.domain.lastOpened.entity.UserDocumentId;
import com.haru.api.domain.lastOpened.entity.UserDocumentLastOpened;
import com.haru.api.domain.lastOpened.entity.enums.DocumentType;
import com.haru.api.domain.lastOpened.repository.UserDocumentLastOpenedRepository;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.user.repository.UserRepository;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.MemberHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDocumentLastOpenedServiceImpl implements UserDocumentLastOpenedService {

    private final UserDocumentLastOpenedRepository userDocumentLastOpenedRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void updateLastOpened(Long userId, DocumentType documentType, Long documentId, Long workspaceId, String title) {
        UserDocumentId id = new UserDocumentId(userId, documentId, documentType);

        UserDocumentLastOpened record = userDocumentLastOpenedRepository.findById(id)
                .orElseGet(() -> {
                    User foundUser = userRepository.findById(userId)
                            .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));
                    return UserDocumentLastOpened.builder()
                            .id(id)
                            .user(foundUser)
                            .workspaceId(workspaceId)
                            .title(title)
                            .build();
                });

        record.updateLastOpened(LocalDateTime.now());
        userDocumentLastOpenedRepository.save(record);

        log.info("userDocumentLastOpened updated for userId: {}, documentId:{}, workspaceId:{}, title:{}", record.getUser().getId(), record.getId().getDocumentId(), workspaceId, title);
    }

}
