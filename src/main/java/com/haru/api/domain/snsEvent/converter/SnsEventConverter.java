package com.haru.api.domain.snsEvent.converter;

import com.haru.api.domain.snsEvent.dto.SnsEventRequestDTO;
import com.haru.api.domain.snsEvent.dto.SnsEventResponseDTO;
import com.haru.api.domain.snsEvent.entity.Participant;
import com.haru.api.domain.snsEvent.entity.SnsEvent;
import com.haru.api.domain.snsEvent.entity.Winner;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.user.entity.enums.Status;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SnsEventConverter {
    public static SnsEvent toSnsEvent(SnsEventRequestDTO.CreateSnsRequest request, User user) {
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

    public static SnsEventResponseDTO.GetSnsEventListRequest toGetSnsEventListRequest(List<SnsEvent> SnsEventList) {
        List<SnsEventResponseDTO.SnsEventList> snsEventList = SnsEventList.stream()
                .map(SnsEventConverter::toSnsEventList)
                .collect(Collectors.toList());

        return SnsEventResponseDTO.GetSnsEventListRequest.builder()
                .snsEventList(snsEventList)
                .build();
    }

    public static SnsEventResponseDTO.SnsEventList toSnsEventList(SnsEvent snsEvent) {
        return SnsEventResponseDTO.SnsEventList.builder()
                .snsEventId(snsEvent.getId())
                .title(snsEvent.getTitle())
                .participantCount(snsEvent.getParticipantList().size())
                .winnerCount(snsEvent.getWinnerList().size())
                .snsLink(snsEvent.getSnsLink())
                .updatedAt(snsEvent.getUpdatedAt())
                .build();
    }
}
