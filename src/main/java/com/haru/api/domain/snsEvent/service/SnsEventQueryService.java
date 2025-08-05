package com.haru.api.domain.snsEvent.service;

import com.haru.api.domain.snsEvent.dto.SnsEventResponseDTO;

public interface SnsEventQueryService {

    SnsEventResponseDTO.GetSnsEventListRequest getSnsEventList(Long userId, Long workspaceId);

    SnsEventResponseDTO.GetSnsEventRequest getSnsEvent(Long userId, Long snsEventId);
}
