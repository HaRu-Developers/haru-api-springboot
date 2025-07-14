package com.haru.api.domain.user.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru.api.global.apiPayload.code.BaseErrorCode;
import com.haru.api.global.apiPayload.code.ErrorReasonDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private BaseErrorCode code;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        response.setContentType("application/json;charset=UTF-8");

        // 커스텀 코드와 메시지 꺼냄
        String errorCode = (String) request.getAttribute("exceptionCode");
        String errorMessage = (String) request.getAttribute("exceptionMessage");
        HttpStatus httpStatus = (HttpStatus) request.getAttribute("exceptionHttpStatus");

        System.out.println("exceptionCode: " + errorCode);
        System.out.println("exceptionMessage: " + errorMessage);
        System.out.println("exceptionHttpStatus: " + httpStatus);

        if (errorCode == null) errorCode = "AUTH9001";
        if (errorMessage == null) errorMessage = "인증에 실패하였습니다.";

        ErrorReasonDTO errorReasonDTO = ErrorReasonDTO.builder()
                .message(errorMessage)
                .code(errorCode)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build();

        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(errorReasonDTO));
    }
}
