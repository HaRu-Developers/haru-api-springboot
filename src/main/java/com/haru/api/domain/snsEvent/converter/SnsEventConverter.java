package com.haru.api.domain.snsEvent.converter;

import com.haru.api.domain.snsEvent.dto.SnsEventRequestDTO;
import com.haru.api.domain.snsEvent.dto.SnsEventResponseDTO;
import com.haru.api.domain.snsEvent.entity.Participant;
import com.haru.api.domain.snsEvent.entity.SnsEvent;
import com.haru.api.domain.snsEvent.entity.Winner;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.user.entity.enums.Status;

import java.util.ArrayList;

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

    public static SnsEventResponseDTO.LinkInstagramAccountResponse toLinkInstagramAccountResponse(String instagramAccountName) {
        return SnsEventResponseDTO.LinkInstagramAccountResponse.builder()
                .instagramAccountName(instagramAccountName)
                .build();
    }
}
