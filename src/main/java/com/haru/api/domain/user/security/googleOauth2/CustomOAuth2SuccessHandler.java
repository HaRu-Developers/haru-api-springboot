package com.haru.api.domain.user.security.googleOauth2;

import com.haru.api.domain.user.service.UserCommandService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Value("${jwt.access-expiration}")
    private int accessExpTime;
    @Value("${jwt.refresh-expiration}")
    private int refreshExpTime;
    private final UserCommandService userCommandService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        String successGoogleLoginUrl = "/auth/login/google/callback";
        CustomOauth2UserDetails userDetails = (CustomOauth2UserDetails) authentication.getPrincipal();
        if (!userDetails.getIsNewUser()) { // 회원가입인지 로그인인지 판단, 로그인이면 토큰 반환
            Long userId = userDetails.getUser().getId();
            String key = "users:" + userId.toString();
            String accessToken = userCommandService.generateAccessToken(userId, accessExpTime);
            String refreshToken = userCommandService.generateAndSaveRefreshToken(key, refreshExpTime);
            // 프론트엔드 URL로 리다이렉트 (query param 전달)
            response.sendRedirect("http://localhost:3000" + successGoogleLoginUrl + "?status=success" + "&accessToken=" + accessToken + "&refreshToken=" + refreshToken);
        } else { // 회원가입
            response.sendRedirect("http://localhost:3000" + successGoogleLoginUrl + "?status=success");
        }
    }
}
