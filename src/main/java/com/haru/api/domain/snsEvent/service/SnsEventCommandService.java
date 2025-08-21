package com.haru.api.domain.snsEvent.service;

import com.haru.api.domain.snsEvent.dto.SnsEventRequestDTO;
import com.haru.api.domain.snsEvent.dto.SnsEventResponseDTO;
import com.haru.api.domain.snsEvent.entity.SnsEvent;
import com.haru.api.domain.snsEvent.entity.enums.Format;
import com.haru.api.domain.snsEvent.entity.enums.InstagramRedirectType;
import com.haru.api.domain.snsEvent.entity.enums.ListType;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.workspace.entity.Workspace;

public interface SnsEventCommandService {
    SnsEventResponseDTO.CreateSnsEventResponse createSnsEvent(User user, Workspace workspace, SnsEventRequestDTO.CreateSnsRequest request);

    SnsEventResponseDTO.LinkInstagramAccountResponse getInstagramAccessTokenAndAccount(String code, Workspace workspace, InstagramRedirectType instagramRedirectType);

    void updateSnsEventTitle(User user, SnsEvent snsEvent, SnsEventRequestDTO.UpdateSnsEventRequest request);

    void deleteSnsEvent(User user, SnsEvent snsEvent);

    SnsEventResponseDTO.ListDownLoadLinkResponse downloadList(User user, SnsEvent snsEvent, ListType listType, Format format);
}
