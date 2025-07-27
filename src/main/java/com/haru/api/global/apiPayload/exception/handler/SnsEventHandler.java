package com.haru.api.global.apiPayload.exception.handler;

import com.haru.api.global.apiPayload.code.BaseErrorCode;
import com.haru.api.global.apiPayload.exception.GeneralException;

public class SnsEventHandler extends GeneralException {
    public SnsEventHandler(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
