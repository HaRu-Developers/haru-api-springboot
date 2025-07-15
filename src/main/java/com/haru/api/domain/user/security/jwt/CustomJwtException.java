package com.haru.api.domain.user.security.jwt;

import com.haru.api.global.apiPayload.code.BaseErrorCode;
import org.springframework.http.HttpStatus;

public class CustomJwtException extends RuntimeException {
    private final BaseErrorCode errorCode;
    private final String code;
    private final HttpStatus httpStatus;

    public CustomJwtException(BaseErrorCode errorCode) {
        super(errorCode.getReasonHttpStatus().getMessage());
        this.errorCode = errorCode;
        this.code = errorCode.getReasonHttpStatus().getCode();
        this.httpStatus = errorCode.getReasonHttpStatus().getHttpStatus();
    }

    public BaseErrorCode getErrorCode() {
        return errorCode;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
