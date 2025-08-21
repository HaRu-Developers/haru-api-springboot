package com.haru.api.domain.snsEvent.service;

import com.haru.api.domain.snsEvent.dto.SnsEventResponseDTO;
import com.haru.api.domain.snsEvent.entity.SnsEvent;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.workspace.entity.Workspace;

public interface SnsEventQueryService {

    SnsEventResponseDTO.GetSnsEventListRequest getSnsEventList(User user, Workspace workspace);

    SnsEventResponseDTO.GetSnsEventRequest getSnsEvent(User user, SnsEvent snsEvent);

    SnsEventResponseDTO.getInstagramAccountName getInstagramAccountName(User user, Workspace workspace);
}
