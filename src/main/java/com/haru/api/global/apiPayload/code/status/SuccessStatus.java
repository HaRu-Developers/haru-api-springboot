package com.haru.api.global.apiPayload.code.status;


import com.haru.api.global.apiPayload.code.BaseCode;
import com.haru.api.global.apiPayload.code.ReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessStatus implements BaseCode {

    // 일반적인 응답
    _OK(HttpStatus.OK, "COMMON200", "성공입니다."),

    // 분위기 트래커 성공 응답
    MOOD_TRACKER_CREATED(HttpStatus.CREATED, "MOODTRACKER201", "분위기 트래커 생성 성공"),
    MOOD_TRACKER_UPDATED(HttpStatus.OK, "MOODTRACKER200", "분위기 트래커 제목 수정 성공"),
    MOOD_TRACKER_DELETED(HttpStatus.NO_CONTENT, "MOODTRACKER204", "분위기 트래커 삭제 성공"),
    MOOD_TRACKER_EMAIL_SENT(HttpStatus.OK, "MOODTRACKER200", "분위기 트래커 설문 링크 전송 성공"),
    MOOD_TRACKER_ANSWER_SUBMIT(HttpStatus.OK, "MOODTRACKER200", "분위기 트래커 설문 답변 제출 성공")
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ReasonDTO getReason() {
        return ReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(true)
                .build();
    }

    @Override
    public ReasonDTO getReasonHttpStatus() {
        return ReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(true)
                .httpStatus(httpStatus)
                .build()
                ;
    }
}