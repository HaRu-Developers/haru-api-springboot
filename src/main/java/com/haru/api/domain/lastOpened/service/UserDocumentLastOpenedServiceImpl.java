package com.haru.api.domain.lastOpened.service;

import com.haru.api.domain.lastOpened.entity.Documentable;
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
import java.util.ArrayList;
import java.util.List;

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
                            .lastOpened(null)
                            .build();
                });

        record.updateLastOpened(LocalDateTime.now());
        userDocumentLastOpenedRepository.save(record);

        log.info("userDocumentLastOpened updated for userId: {}, documentId:{}, workspaceId:{}, title:{}", record.getUser().getId(), record.getId().getDocumentId(), workspaceId, title);
    }

    public void createInitialRecordsForWorkspaceUsers(List<User> usersInWorkspace, Documentable document) {

        // 저장할 엔티티들을 담을 리스트를 생성
        List<UserDocumentLastOpened> recordsToSave = new ArrayList<>();

        // 각 유저에 대해 last opened entity를 생성하고 리스트에 추가
        for (User user : usersInWorkspace) {
            UserDocumentId documentId = new UserDocumentId(
                    user.getId(),
                    document.getId(),
                    document.getDocumentType()
            );

            UserDocumentLastOpened newRecord = UserDocumentLastOpened.builder()
                    .id(documentId)
                    .user(user)
                    .title(document.getTitle())
                    .workspaceId(document.getWorkspaceId())
                    .lastOpened(null)
                    .build();

            recordsToSave.add(newRecord);
        }

        if (!recordsToSave.isEmpty()) {
            userDocumentLastOpenedRepository.saveAll(recordsToSave);
        }
    }

}
