package com.haru.apiPayload.exception.handler;


import com.haru.apiPayload.code.BaseErrorCode;
import com.haru.apiPayload.exception.GeneralException;

public class TempHandler extends GeneralException {

    public TempHandler(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
