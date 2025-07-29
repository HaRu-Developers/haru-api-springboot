package com.haru.api.global.apiPayload.exception.handler;

import com.haru.api.global.apiPayload.code.BaseErrorCode;
import com.haru.api.global.apiPayload.exception.GeneralException;

public class MoodTrackerHandler extends GeneralException {

  public MoodTrackerHandler(BaseErrorCode errorCode) {
    super(errorCode);
  }
}