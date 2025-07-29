package com.haru.api.infra.mail.handler;

import com.haru.api.global.apiPayload.code.BaseErrorCode;
import com.haru.api.global.apiPayload.exception.GeneralException;

public class MailHandler extends GeneralException {

    public MailHandler(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
