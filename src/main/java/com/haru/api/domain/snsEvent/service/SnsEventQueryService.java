package com.haru.api.domain.snsEvent.service;

import com.haru.api.domain.snsEvent.dto.SnsEventResponseDTO;

public interface SnsEventQueryService {

    SnsEventResponseDTO.GetSnsEventListRequest getSnsEventList(Long userId, String workspaceId);

    SnsEventResponseDTO.GetSnsEventRequest getSnsEvent(Long userId, String snsEventId);
}
