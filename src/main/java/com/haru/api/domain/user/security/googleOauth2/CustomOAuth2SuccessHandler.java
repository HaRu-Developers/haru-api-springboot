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
    @Value("${google-login-frontend-url}")
    String baseUrl;
    private final UserCommandService userCommandService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        StringBuilder redirectUrl = new StringBuilder();
        String successGoogleLoginUrl = "/auth/login/google/callback";
        CustomOauth2UserDetails userDetails = (CustomOauth2UserDetails) authentication.getPrincipal();
        // 회원가입이든 로그인이든 똑같이 프론트엔드로 리다이렉트
        Long userId = userDetails.getUser().getId();
        String key = "users:" + userId.toString();
        String accessToken = userCommandService.generateAccessToken(userId, accessExpTime);
        String refreshToken = userCommandService.generateAndSaveRefreshToken(key, refreshExpTime);
        // 프론트엔드 URL로 리다이렉트 (query param 전달)
        redirectUrl.append(baseUrl)
                .append(successGoogleLoginUrl)
                .append("?status=success")
                .append("&userId=").append(userId)
                .append("&profileImage=").append(userDetails.getUser().getProfileImage())
                .append("&accessToken=").append(accessToken)
                .append("&refreshToken=").append(refreshToken);
        response.sendRedirect(redirectUrl.toString());
    }
}
