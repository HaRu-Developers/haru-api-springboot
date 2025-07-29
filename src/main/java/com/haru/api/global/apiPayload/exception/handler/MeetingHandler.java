package com.haru.api.global.apiPayload.exception.handler;

import com.haru.api.global.apiPayload.code.BaseErrorCode;
import com.haru.api.global.apiPayload.exception.GeneralException;

public class MeetingHandler extends GeneralException {

    public MeetingHandler(BaseErrorCode errorCode) {
        super(errorCode);
    }
}

