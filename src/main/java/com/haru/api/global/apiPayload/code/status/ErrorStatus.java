package com.haru.api.global.apiPayload.code.status;


import com.haru.api.global.apiPayload.code.BaseErrorCode;
import com.haru.api.global.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {

    // 가장 일반적인 응답
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST,"COMMON400","잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"COMMON401","인증이 필요합니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),


    // 회원 관려 에러 1000
    MEMBER_NOT_FOUND(HttpStatus.BAD_REQUEST, "MEMBER1001", "사용자가 없습니다."),
    REFRESH_TOKEN_NOT_EQUAL(HttpStatus.BAD_REQUEST, "MEMBER1002", "리프레시 토큰이 일치하지 않습니다."),

    // Workspace 관련 에러 2000
    WORKSPACE_NOT_FOUND(HttpStatus.BAD_REQUEST,"WORKSPACE2001", "워크스페이스가 없습니다."),
    WORKSPACE_MODIFY_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "WORKSPACE2002", "워크스페이스 수정 권한이 없습니다."),
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "INVITATION2003", "초대 코드에 해당하는 초대장이 존재하지 않습니다."),
    EMAIL_MISMATCH(HttpStatus.BAD_REQUEST, "INVITATION2004", "초대장의 이메일과 현재 유저의 이메일이 일치하지 않습니다."),
    ALREADY_ACCEPTED(HttpStatus.BAD_REQUEST, "INVITATION2005", "이미 초대가 수락된 초대장입니다."),

    // AI회의 Meetings 관련 에러 3000
    MEETING_FILE_NOT_FOUND(HttpStatus.BAD_REQUEST, "MEETING3001", "안건지가 업로드되지 않았습니다."),
  
    // 인가 관련 에러 9000
    AUTHORIZATION_EXCEPTION(HttpStatus.UNAUTHORIZED, "AUTHORIZATION9001", "인증에 실패하였습니다."),
    JWT_ACCESSTOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTHORIZATION9002", "AccessToekn이 만료되었습니다."),
    JWT_REFRESHTOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTHORIZATION9003", "RefreshToekn이 만료되었습니다."),
    LOGOUT_USER(HttpStatus.UNAUTHORIZED, "AUTHORIZATION9004", "로그아웃된 유저입니다."),
    JWT_TOKEN_NOT_RECEIVED(HttpStatus.UNAUTHORIZED, "AUTHORIZATION9005", "JWT 토큰이 전달되지 않았습니다."),
    JWT_TOKEN_OUT_OF_FORM(HttpStatus.UNAUTHORIZED, "AUTHORIZATION9006", "JWT 토큰의 형식이 올바르지 않습니다.");
  

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
                .build();
    }
}
