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
                            .build();
                });

        record.updateLastOpened(LocalDateTime.now());
        userDocumentLastOpenedRepository.save(record);

        log.info("userDocumentLastOpened updated for userId: {}, documentId:{}, workspaceId:{}, title:{}", record.getUser().getId(), record.getId().getDocumentId(), workspaceId, title);
    }

    @Override
    public void createInitialRecordsForWorkspaceUsers(List<User> usersInWorkspace, Documentable document) {

        // 저장할 엔티티 리스트 생성
        List<UserDocumentLastOpened> recordsToSave = recordsToProcess(usersInWorkspace, document);

        // 전체 save
        if (!recordsToSave.isEmpty()) {
            userDocumentLastOpenedRepository.saveAll(recordsToSave);
        }
    }

    @Override
    public void deleteRecordsForWorkspaceUsers(Documentable documentable) {

        // 해당 문서 id에 해당하는 last opened 튜플 검색
        List<UserDocumentLastOpened> recordsToUpdate = userDocumentLastOpenedRepository.findById_DocumentId(documentable.getId());

        if (!recordsToUpdate.isEmpty()) {
            userDocumentLastOpenedRepository.deleteAllInBatch(recordsToUpdate);
        }

    }

    @Override
    public void updateRecordsForWorkspaceUsers(Documentable documentable) {

        // 해당 문서 id에 해당하는 last opened 튜플 검색
        List<UserDocumentLastOpened> recordsToUpdate = userDocumentLastOpenedRepository.findById_DocumentId(documentable.getId());

        if (!recordsToUpdate.isEmpty()) {
            for (UserDocumentLastOpened record : recordsToUpdate) {
                record.updateTitle(documentable.getTitle());
            }
        }
    }

    private List<UserDocumentLastOpened> recordsToProcess(List<User> usersInWorkspace, Documentable document) {
        // 처리할 엔티티들을 담을 리스트 생성
        List<UserDocumentLastOpened> recordsToProcess = new ArrayList<>();

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
                    .build();

            recordsToProcess.add(newRecord);
        }

        return recordsToProcess;
    }

}
