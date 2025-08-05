package com.haru.api.domain.snsEvent.converter;

import com.haru.api.domain.snsEvent.dto.SnsEventRequestDTO;
import com.haru.api.domain.snsEvent.dto.SnsEventResponseDTO;
import com.haru.api.domain.snsEvent.entity.Participant;
import com.haru.api.domain.snsEvent.entity.SnsEvent;
import com.haru.api.domain.snsEvent.entity.Winner;
import com.haru.api.domain.user.entity.User;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SnsEventConverter {
    public static SnsEvent toSnsEvent(
            SnsEventRequestDTO.CreateSnsRequest request,
            User user
    ) {
        return SnsEvent.builder()
                .title(request.getTitle())
                .snsLink(request.getSnsEventLink())
                .creator(user)
                .participantList(new ArrayList<>())
                .winnerList(new ArrayList<>())
                .build();
    }

    public static Participant toParticipant(String nickname) {
        return Participant.builder()
                .nickname(nickname)
                .build();
    }

    public static Winner toWinner(String nickname) {
        return Winner.builder()
                .nickname(nickname)
                .build();
    }

    public static SnsEventResponseDTO.LinkInstagramAccountResponse toLinkInstagramAccountResponse(String instagramAccountName) {
        return SnsEventResponseDTO.LinkInstagramAccountResponse.builder()
                .instagramAccountName(instagramAccountName)
                .build();
    }
          
    public static SnsEventResponseDTO.GetSnsEventListRequest toGetSnsEventListRequest(List<SnsEvent> snsEventList) {
        List<SnsEventResponseDTO.SnsEventResponse> snsEventResponseList = snsEventList.stream()
                .map(SnsEventConverter::toSnsEventList)
                .collect(Collectors.toList());

        return SnsEventResponseDTO.GetSnsEventListRequest.builder()
                .snsEventList(snsEventResponseList)
                .build();
    }

    public static SnsEventResponseDTO.SnsEventResponse toSnsEventList(SnsEvent snsEvent) {
        return SnsEventResponseDTO.SnsEventResponse.builder()
                .snsEventId(snsEvent.getId().toString())
                .title(snsEvent.getTitle())
                .participantCount(snsEvent.getParticipantList().size())
                .winnerCount(snsEvent.getWinnerList().size())
                .snsLink(snsEvent.getSnsLink())
                .updatedAt(snsEvent.getUpdatedAt())
                .build();
    }

    public static SnsEventResponseDTO.GetSnsEventRequest toGetSnsEventRequest(
            SnsEvent snsEvent,
            List<Participant> participantList,
            List<Winner> winnerList
    ) {
        List<SnsEventResponseDTO.ParticipantResponse> participantResponseList = participantList.stream()
                .map(SnsEventConverter::toParticipantResponse)
                .collect(Collectors.toList());
        List<SnsEventResponseDTO.WinnerResponse> winnerResponseList = winnerList.stream()
                .map(SnsEventConverter::toWinnerResponse)
                .collect(Collectors.toList());

        return SnsEventResponseDTO.GetSnsEventRequest.builder()
                .title(snsEvent.getTitle())
                .creatorId(snsEvent.getCreator().getId().toString())
                .creatorName(snsEvent.getCreator().getName())
                .updatedAt(snsEvent.getUpdatedAt())
                .participantList(participantResponseList)
                .winnerList(winnerResponseList)
                .snsLink(snsEvent.getSnsLink())
                .workspaceId(snsEvent.getWorkspace().getId().toString())
                .build();
    }

    public static SnsEventResponseDTO.ParticipantResponse toParticipantResponse(Participant participant) {
        return SnsEventResponseDTO.ParticipantResponse.builder()
                .account(participant.getNickname())
                .build();
    }

    public static SnsEventResponseDTO.WinnerResponse toWinnerResponse(Winner winner) {
        return SnsEventResponseDTO.WinnerResponse.builder()
                .account(winner.getNickname())
                .build();
    }
}
