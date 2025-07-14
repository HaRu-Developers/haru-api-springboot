package com.haru.api.global.apiPayload.exception.handler;

import com.haru.api.global.apiPayload.code.BaseErrorCode;
import com.haru.api.global.apiPayload.exception.GeneralException;

public class WorkspaceInvitationHandler extends GeneralException {
    public WorkspaceInvitationHandler(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
