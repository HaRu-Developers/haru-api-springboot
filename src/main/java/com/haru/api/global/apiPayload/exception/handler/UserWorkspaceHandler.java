package com.haru.api.global.apiPayload.exception.handler;

import com.haru.api.global.apiPayload.code.BaseErrorCode;
import com.haru.api.global.apiPayload.exception.GeneralException;

public class UserWorkspaceHandler extends GeneralException {
    public UserWorkspaceHandler(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
