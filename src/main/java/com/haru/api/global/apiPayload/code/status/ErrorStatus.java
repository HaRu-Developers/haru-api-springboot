package com.haru.api.global.apiPayload.code.status;


import com.haru.api.global.apiPayload.code.BaseErrorCode;
import com.haru.api.global.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {

    // For test
    TEMP_EXCEPTION(HttpStatus.BAD_REQUEST, "TEMP4001", "이거는 테스트"),

    // 가장 일반적인 응답
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST,"COMMON400","잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"COMMON401","인증이 필요합니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),


    // 회원 관려 에러 1000
    MEMBER_NOT_FOUND(HttpStatus.BAD_REQUEST, "MEMBER1001", "사용자가 없습니다."),
    REFRESHTOKEN_NOT_EQUAL(HttpStatus.BAD_REQUEST, "MEMBER1002", "리프레시 토큰이 일치하지 않습니다."),

    // Workspace 관련 에러
    WORKSPACE_NOT_FOUND(HttpStatus.BAD_REQUEST,"WORKSPACE1001", "워크스페이스가 없습니다."),
    WORKSPACE_MODIFY_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "WORKSPACE1002", "워크스페이스 수정 권한이 없습니다."),

    // AI회의 Meetings 관련 에러
    MEETING_FILE_NOT_FOUND(HttpStatus.BAD_REQUEST, "MEETING1001", "안건지가 업로드되지 않았습니다."),

    // workspace 초대 관련 에러
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "INVITATION1001", "초대 코드에 해당하는 초대장이 존재하지 않습니다."),
    EMAIL_MISMATCH(HttpStatus.BAD_REQUEST, "INVITATION1002", "초대장의 이메일과 현재 유저의 이메일이 일치하지 않습니다."),
    ALREADY_ACCEPTED(HttpStatus.BAD_REQUEST, "INVITATION1003", "이미 초대가 수락된 초대장입니다."),

    // 예시,,,
    ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "ARTICLE4001", "게시글이 없습니다.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .build();
    }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build()
                ;
    }
}
