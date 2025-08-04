package com.haru.api.global.apiPayload.exception.handler;

import com.haru.api.global.apiPayload.code.BaseErrorCode;
import com.haru.api.global.apiPayload.exception.GeneralException;

public class UserDocumentLastOpenedHandler extends GeneralException {
    public UserDocumentLastOpenedHandler(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
