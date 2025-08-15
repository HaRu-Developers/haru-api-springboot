package com.haru.api.domain.user.security.googleOauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomOAuth2FailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        String failureGoogleLoginUrl = "/auth/login/google/callback";
        // 프론트엔드 URL로 리다이렉트 (query param 전달)
        response.sendRedirect("https://org.haru.it.kr" + failureGoogleLoginUrl + "?status=fail");
    }
}
