package com.haru.api.domain.snsEvent.service;

import com.haru.api.domain.snsEvent.dto.SnsEventRequestDTO;
import com.haru.api.domain.snsEvent.dto.SnsEventResponseDTO;
import com.haru.api.domain.snsEvent.entity.enums.Format;
import com.haru.api.domain.snsEvent.entity.enums.ListType;

public interface SnsEventCommandService {
    SnsEventResponseDTO.CreateSnsEventResponse createSnsEvent(Long workspaceId, SnsEventRequestDTO.CreateSnsRequest request);

    SnsEventResponseDTO.LinkInstagramAccountResponse getInstagramAccessTokenAndAccount(String code, Long workspaceId);

    void updateSnsEventTitle(Long userId, Long snsEventId, SnsEventRequestDTO.UpdateSnsEventRequest request);

    void deleteSnsEvent(Long userId, Long snsEventId);

    SnsEventResponseDTO.ListDownLoadLinkResponse downloadList(Long userId, Long snsEventId, ListType listType, Format format);
}
