package com.haru.api.global.apiPayload.exception.handler;

import com.haru.api.global.apiPayload.code.BaseErrorCode;
import com.haru.api.global.apiPayload.exception.GeneralException;

public class WorkspaceHandler extends GeneralException {
    public WorkspaceHandler(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
