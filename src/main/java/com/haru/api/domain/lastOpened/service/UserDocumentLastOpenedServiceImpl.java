package com.haru.api.domain.lastOpened.service;

import com.haru.api.domain.lastOpened.entity.Documentable;
import com.haru.api.domain.lastOpened.entity.UserDocumentId;
import com.haru.api.domain.lastOpened.entity.UserDocumentLastOpened;
import com.haru.api.domain.lastOpened.repository.UserDocumentLastOpenedRepository;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.user.repository.UserRepository;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.MemberHandler;
import com.haru.api.global.common.entity.TitleHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateLastOpened(UserDocumentId userDocumentId, Long workspaceId, String title) {

        UserDocumentLastOpened record = userDocumentLastOpenedRepository.findById(userDocumentId)
                .orElseGet(() -> {
                    User foundUser = userRepository.findById(userDocumentId.getUserId())
                            .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));
                    return UserDocumentLastOpened.builder()
                            .id(userDocumentId)
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
    @Transactional
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

        // 해당 문서 id, 문서 타입에 해당하는 last opened 튜플 검색
        List<UserDocumentLastOpened> recordsToUpdate = userDocumentLastOpenedRepository.findByDocumentIdAndDocumentType(documentable.getId(), documentable.getDocumentType());

        if (!recordsToUpdate.isEmpty()) {
            userDocumentLastOpenedRepository.deleteAllInBatch(recordsToUpdate);
        }

    }

    @Override
    @Transactional
    public void updateRecordsForWorkspaceUsers(Documentable documentable, TitleHolder titleHolder) {

        // 해당 문서 id, 문서 타입에 해당하는 last opened 튜플 검색
        List<UserDocumentLastOpened> recordsToUpdate = userDocumentLastOpenedRepository.findByDocumentIdAndDocumentType(documentable.getId(), documentable.getDocumentType());

        if (!recordsToUpdate.isEmpty()) {
            for (UserDocumentLastOpened record : recordsToUpdate) {
                record.updateTitle(titleHolder.getTitle());
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
                    .thumbnailKeyName(document.getThumbnailKeyName())
                    .build();

            recordsToProcess.add(newRecord);
        }

        return recordsToProcess;
    }

    @Override
    @Transactional
    public void updateRecordsTitleAndThumbnailForWorkspaceUsers(List<User> usersInWorkspace, Documentable documentable, TitleHolder titleHolder) {
        // 해당 문서 id, 문서 타입에 해당하는 last opened 튜플 검색
        List<UserDocumentLastOpened> recordsToUpdate = userDocumentLastOpenedRepository.findByDocumentIdAndDocumentType(documentable.getId(), documentable.getDocumentType());

        if (!recordsToUpdate.isEmpty()) {
            for (UserDocumentLastOpened record : recordsToUpdate) {
                record.updateTitle(titleHolder.getTitle());
                record.updateThumbnailKeyName(documentable.getThumbnailKeyName());
            }
        }
    }

    @Override
    @Transactional
    public void updateRecordsThumbnailForWorkspaceUsers(List<User> usersInWorkspace, Documentable documentable) {
        // 해당 문서 id, 문서 타입에 해당하는 last opened 튜플 검색
        List<UserDocumentLastOpened> recordsToUpdate = userDocumentLastOpenedRepository.findByDocumentIdAndDocumentType(documentable.getId(), documentable.getDocumentType());

        if (!recordsToUpdate.isEmpty()) {
            for (UserDocumentLastOpened record : recordsToUpdate) {
                record.updateThumbnailKeyName(documentable.getThumbnailKeyName());
            }
        }
    }

}
