package com.haru.api.domain.snsEvent.service;

import com.haru.api.domain.lastOpened.entity.enums.DocumentType;
import com.haru.api.domain.snsEvent.converter.SnsEventConverter;
import com.haru.api.domain.snsEvent.dto.SnsEventResponseDTO;
import com.haru.api.domain.snsEvent.entity.Participant;
import com.haru.api.domain.snsEvent.entity.SnsEvent;
import com.haru.api.domain.snsEvent.entity.Winner;
import com.haru.api.domain.snsEvent.repository.ParticipantRepository;
import com.haru.api.domain.snsEvent.repository.SnsEventRepository;
import com.haru.api.domain.snsEvent.repository.WinnerRepository;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.user.repository.UserRepository;
import com.haru.api.domain.userWorkspace.entity.UserWorkspace;
import com.haru.api.domain.userWorkspace.repository.UserWorkspaceRepository;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.domain.workspace.repository.WorkspaceRepository;
import com.haru.api.global.annotation.TrackLastOpened;
import com.haru.api.global.apiPayload.exception.handler.MemberHandler;
import com.haru.api.global.apiPayload.exception.handler.SnsEventHandler;
import com.haru.api.global.apiPayload.exception.handler.WorkspaceHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.haru.api.global.apiPayload.code.status.ErrorStatus.*;

@Service
@RequiredArgsConstructor
public class SnsEventQueryServiceImpl implements SnsEventQueryService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserWorkspaceRepository userWorkspaceRepository;
    private final SnsEventRepository snsEventRepository;
    private final ParticipantRepository participantRepository;
    private final WinnerRepository winnerRepository;

    @Override
    public SnsEventResponseDTO.GetSnsEventListRequest getSnsEventList(Long userId, Long workspaceId) {
        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(MEMBER_NOT_FOUND));
        Workspace foundWorkspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceHandler(WORKSPACE_NOT_FOUND));
        UserWorkspace foundUserWorkSpace = userWorkspaceRepository.findByUserAndWorkspace(foundUser, foundWorkspace)
                .orElseThrow(() -> new MemberHandler(NOT_BELONG_TO_WORKSPACE));
        List<SnsEvent> snsEventList = snsEventRepository.findAllByWorkspace(foundWorkspace);
        return SnsEventConverter.toGetSnsEventListRequest(snsEventList);
    }

    @Override
    @TrackLastOpened(type = DocumentType.SNS_EVENT_ASSISTANT)
    public SnsEventResponseDTO.GetSnsEventRequest getSnsEvent(Long userId, Long snsEventId) {
        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(MEMBER_NOT_FOUND));
        SnsEvent foundSnsEvent = snsEventRepository.findById(snsEventId)
                .orElseThrow(() -> new SnsEventHandler(SNS_EVENT_NOT_FOUND));
        List<Participant> participantList = participantRepository.findAllBySnsEvent(foundSnsEvent);
        List<Winner> winnerList = winnerRepository.findAllBySnsEvent(foundSnsEvent);
        return SnsEventConverter.toGetSnsEventRequest(
                foundSnsEvent,
                participantList,
                winnerList
        );
    }
}
