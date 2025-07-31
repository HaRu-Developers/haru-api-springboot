package com.haru.api.domain.snsEvent.service;

import com.haru.api.domain.snsEvent.dto.SnsEventRequestDTO;
import com.haru.api.domain.snsEvent.dto.SnsEventResponseDTO;

public interface SnsEventCommandService {
    SnsEventResponseDTO.CreateSnsEventResponse createSnsEvent(Long workspaceId, SnsEventRequestDTO.CreateSnsRequest request);

    void updateSnsEvent(Long userId, Long snsEvnetId, SnsEventRequestDTO.UpdateSnsEventRequest request);
}
