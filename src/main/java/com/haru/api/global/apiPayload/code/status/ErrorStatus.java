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

    // 회원 관려 에러
    MEMBER_NOT_FOUND(HttpStatus.BAD_REQUEST, "MEMBER4001", "사용자가 없습니다."),
    REFRESH_TOKEN_NOT_EQUAL(HttpStatus.BAD_REQUEST, "MEMBER4002", "리프레시 토큰이 일치하지 않습니다."),
    MEMBER_NO_AUTHORITY(HttpStatus.FORBIDDEN, "MEMBER4003", "수정 및 삭제할 권한이 없습니다."),
    MEMBER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "MEMBER4004", "이미 존재하는 회원입니다."),
    MEMBER_USERNAME_NOT_FOUND(HttpStatus.BAD_REQUEST, "MEMBER4005", "해당 아이디를 가진 유저가 존재하지 않습니다."), //
    MEMBER_PASSWORD_NOT_MATCH(HttpStatus.BAD_REQUEST, "MEMBER4006", "비밀번호가 일치하지 않습니다."),
    SAME_WITH_OLD_PASSWORD(HttpStatus.BAD_REQUEST, "MEMBER4007", "변경하고자하는 비밀번호와 이전 비밀번호가 일치합니다."),

    // Workspace 관련 에러
    WORKSPACE_NOT_FOUND(HttpStatus.BAD_REQUEST,"WORKSPACE4001", "워크스페이스가 없습니다."),
    WORKSPACE_MODIFY_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "WORKSPACE4002", "워크스페이스 수정 권한이 없습니다."),
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "WORKSPACE4003", "초대 코드에 해당하는 초대장이 존재하지 않습니다."),
    EMAIL_MISMATCH(HttpStatus.BAD_REQUEST, "WORKSPACE4004", "초대장의 이메일과 현재 유저의 이메일이 일치하지 않습니다."),
    ALREADY_ACCEPTED(HttpStatus.BAD_REQUEST, "WORKSPACE4005", "이미 초대가 수락된 초대장입니다."),
    NOT_BELONG_TO_WORKSPACE(HttpStatus.UNAUTHORIZED, "WORKSPACE4006", "해당 워크스페이스에 속해있지 않습니다."),
    WORKSPACE_CREATOR_NOT_FOUND(HttpStatus.BAD_REQUEST, "WORKSPACE4007", "워크스페이스 생성자를 찾을 수 없습니다."),

    // UserWorkspace 관련 에러
    USER_WORKSPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "USERWORKSPACE4001", "해당 유저가 해당 워크스페이스에 속해있지 않습니다."),

    // AI회의 Meetings 관련 에러
    MEETING_NOT_FOUND(HttpStatus.BAD_REQUEST, "MEETING4001","회의를 찾을 수 없습니다."),
    MEETING_FILE_NOT_FOUND(HttpStatus.BAD_REQUEST, "MEETING4002", "안건지가 업로드되지 않았습니다."),

    // 인가 관련 에러
    AUTHORIZATION_EXCEPTION(HttpStatus.UNAUTHORIZED, "AUTHORIZATION4001", "인증에 실패하였습니다."),
    JWT_ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTHORIZATION4002", "AccessToken이 만료되었습니다."),
    JWT_REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTHORIZATION4003", "RefreshToken이 만료되었습니다."),
    LOGOUT_USER(HttpStatus.UNAUTHORIZED, "AUTHORIZATION4004", "로그아웃된 유저입니다."),
    JWT_TOKEN_NOT_RECEIVED(HttpStatus.UNAUTHORIZED, "AUTHORIZATION4005", "JWT 토큰이 전달되지 않았습니다."),
    JWT_TOKEN_OUT_OF_FORM(HttpStatus.UNAUTHORIZED, "AUTHORIZATION4006", "JWT 토큰의 형식이 올바르지 않습니다."),

    // 분위기 트래커 관련 에러
    MOOD_TRACKER_NOT_FOUND(HttpStatus.BAD_REQUEST,"MOODTRACKER4001", "분위기 트래커가 없습니다."),
    MOOD_TRACKER_MODIFY_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "MOODTRACKER4002", "분위기 트래커에 권한이 없습니다."),
    SURVEY_QUESTION_NOT_FOUND(HttpStatus.BAD_REQUEST, "MOODTRACKER4003", "분위기 트래커 설문에 없는 질문입니다."),
    SURVEY_ANSWER_REQUIRED(HttpStatus.BAD_REQUEST, "MOODTRACKER4004", "분위기 트래커 설문의 필수 응답이 누락되었습니다."),
    MOOD_TRACKER_NOT_FINISHED(HttpStatus.BAD_REQUEST, "MOODTRACKER4005", "분위기 트래커 설문 마감일 전입니다."),
    MOOD_TRACKER_ACCESS_DENIED(HttpStatus.BAD_REQUEST, "MOODTRACKER4006", "분위기 트래커 조회 권한이 없습니다."),

    // 메일 관련 에러
    MAIL_SEND_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "MAIL500", "이메일 전송에 실패했습니다."),

    // SNS 이벤트 관련 에러
    SNS_EVENT_NOT_FOUND(HttpStatus.BAD_REQUEST, "SNSEVENT4001", "SNS 이벤트가 존재하지 않습니다."),
    SNS_EVENT_LINK_NOT_FOUND(HttpStatus.BAD_REQUEST, "SNS_EVENT4002", "잘못된 인스타그램 게시물 링크 형식입니다."),
    SNS_EVENT_INSTAGRAM_API_ERROR(HttpStatus.BAD_REQUEST, "SNS_EVENT4003", "인스타그램 API 호출에 실패했습니다."),
    SNS_EVENT_INSTAGRAM_API_NO_MEDIA(HttpStatus.BAD_REQUEST, "SNS_EVENT4004", "인스타그램 게시물에 미디어가 없습니다."),
    SNS_EVENT_INSTAGRAM_API_NO_COMMENT(HttpStatus.BAD_REQUEST, "SNS_EVENT4005", "인스타그램 게시물에 댓글이 없습니다."),
    SNS_EVENT_NO_ACCESS_TOKEN(HttpStatus.BAD_REQUEST, "SNS_EVENT4006", "인스타그램 액세스 토큰이 없습니다."),
    SNS_EVENT_NO_AUTHORITY(HttpStatus.UNAUTHORIZED, "SNS_EVENT4007", "인스타그램 이벤트에 대한 수정 권한이 없습니다."),
    SNS_EVENT_INSTAGRAM_ALREADY_LINKED(HttpStatus.BAD_REQUEST, "SNS_EVENT4008", "이미 연동된 인스타그램 계정입니다."),
  
    // last opened 관련 에러
    USER_DOCUMENT_LAST_OPENED_NOT_FOUND(HttpStatus.NOT_FOUND, "LASTOPENED4001", "해당 문서에 대한 마지막 조회 데이터가 존재하지 않습니다."),

    // 약관 관련 에러
    TERM_NOT_FOUND(HttpStatus.NOT_FOUND, "TERM4004", "요청한 약관이 존재하지 않습니다.");

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
