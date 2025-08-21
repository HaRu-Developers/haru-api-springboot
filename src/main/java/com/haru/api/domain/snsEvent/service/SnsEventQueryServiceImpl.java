package com.haru.api.domain.snsEvent.service;

import com.haru.api.domain.snsEvent.converter.SnsEventConverter;
import com.haru.api.domain.snsEvent.dto.SnsEventResponseDTO;
import com.haru.api.domain.snsEvent.entity.Participant;
import com.haru.api.domain.snsEvent.entity.SnsEvent;
import com.haru.api.domain.snsEvent.entity.Winner;
import com.haru.api.domain.snsEvent.repository.ParticipantRepository;
import com.haru.api.domain.snsEvent.repository.SnsEventRepository;
import com.haru.api.domain.snsEvent.repository.WinnerRepository;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.global.annotation.TrackLastOpened;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SnsEventQueryServiceImpl implements SnsEventQueryService {

    private final SnsEventRepository snsEventRepository;
    private final ParticipantRepository participantRepository;
    private final WinnerRepository winnerRepository;

    @Override
    public SnsEventResponseDTO.GetSnsEventListRequest getSnsEventList(User user, Workspace workspace) {

        List<SnsEvent> snsEventList = snsEventRepository.findAllByWorkspaceOrderByUpdatedAtDesc(workspace);

        return SnsEventConverter.toGetSnsEventListRequest(snsEventList);

    }

    @Override
    @TrackLastOpened
    public SnsEventResponseDTO.GetSnsEventRequest getSnsEvent(User user, SnsEvent snsEvent) {

        List<Participant> participantList = participantRepository.findAllBySnsEvent(snsEvent);

        List<Winner> winnerList = winnerRepository.findAllBySnsEvent(snsEvent);

        return SnsEventConverter.toGetSnsEventRequest(
                snsEvent,
                participantList,
                winnerList
        );

    }

    @Override
    public SnsEventResponseDTO.getInstagramAccountName getInstagramAccountName(User user, Workspace workspace) {
        if (workspace.getInstagramAccountName() == null) {
            return SnsEventConverter.toGetInstagramAccountName(
                    ""
            );
        }
        return SnsEventConverter.toGetInstagramAccountName(
                workspace.getInstagramAccountName()
        );
    }
}
