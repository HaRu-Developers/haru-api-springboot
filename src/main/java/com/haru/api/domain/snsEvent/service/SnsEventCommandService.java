package com.haru.api.domain.snsEvent.service;

import com.haru.api.domain.snsEvent.dto.SnsEventRequestDTO;
import com.haru.api.domain.snsEvent.dto.SnsEventResponseDTO;

public interface SnsEventCommandService {
    SnsEventResponseDTO.CreateSnsEventResponse createSnsEvent(String workspaceId, SnsEventRequestDTO.CreateSnsRequest request);

    SnsEventResponseDTO.LinkInstagramAccountResponse getInstagramAccessTokenAndAccount(String code, String workspaceId);

    void updateSnsEventTitle(Long userId, String snsEventId, SnsEventRequestDTO.UpdateSnsEventRequest request);

    void deleteSnsEvent(Long userId, String snsEventId);
}
