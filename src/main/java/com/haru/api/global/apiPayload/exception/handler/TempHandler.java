package com.haru.api.global.apiPayload.exception.handler;


import com.haru.api.global.apiPayload.code.BaseErrorCode;
import com.haru.api.global.apiPayload.exception.GeneralException;

public class TempHandler extends GeneralException {

    public TempHandler(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
