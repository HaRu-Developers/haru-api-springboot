package com.haru.api.domain.user.security.jwt;

import com.google.gson.Gson;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

@RequiredArgsConstructor
@Component
@Slf4j //???
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.header}")
    private String jwtHeader;
    private static final String[] whitelist = {
            "/api/v1/users/signup",
            "/api/v1/users/login",
            "/swagger-ui/**",
            "/v3/**",
            "/users/admin/**"
    };
    private final JwtUtils jwtUtils;
    private final RedisTemplate<String, String> redisTemplate;

    private static void checkAuthorizationHeader(String header) {
        log.info("-------------------#@@@@@------------------");
        if(header == null) {
            throw new CustomJwtException(ErrorStatus.JWT_TOKEN_NOT_RECEIVED);
        } else if (!header.startsWith("Bearer ")) {
            throw new CustomJwtException(ErrorStatus.JWT_TOKEN_OUT_OF_FORM);
        }
    }

    // 필터를 거치지 않을 URL(로그인, 회원가입) 을 설정하고, true 를 return 하면 현재 필터를 건너뛰고 다음 필터로 이동
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String requestURI = request.getRequestURI();
        log.info("---------------$$$$$$------------------");
        log.info("boolean: {}", PatternMatchUtils.simpleMatch(whitelist, requestURI));
        log.info("requestURI: {}", requestURI);
        return PatternMatchUtils.simpleMatch(whitelist, requestURI);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(jwtHeader);

        String requestUri = request.getRequestURI();
        String refreshToken = request.getHeader("refreshToken");

        // 특정 경로(토큰 갱신 api)에서만 Refresh Token 처리
        if ("/api/v1/users/refresh".equals(requestUri)) {
            if (refreshToken != null) {
                try {
                    // access token 검증
                    checkAuthorizationHeader(authHeader);   // header 가 올바른 형식인지 체크
                    String token = JwtUtils.getTokenFromHeader(authHeader);
                    Claims claims = jwtUtils.validateTokenOnlySignature(token); // 토큰 검증
                    Authentication authentication = jwtUtils.getAuthenticationFromExpiredAccessToken(token); // 사용자 인증 정보 생성
                    SecurityContextHolder.getContext().setAuthentication(authentication); // 사용자 인증 정보 저장
                    // refresh token 검증
                    jwtUtils.validateRefreshToken(refreshToken); // 토큰 검증
                    filterChain.doFilter(request, response);    // 다음 필터로 이동
                } catch (Exception e) {
                    Gson gson = new Gson();
                    String json = "";
                    if (e instanceof CustomJwtException) {
                        request.setAttribute("exceptionCode", ((CustomJwtException) e).getCode());
                        request.setAttribute("exceptionMessage", e.getMessage());
                        request.setAttribute("exceptionHttpStatus", ((CustomJwtException) e).getHttpStatus());
                        throw new InsufficientAuthenticationException(e.getMessage()); // EntryPoint로 넘김
                    }
                    else {
                        json = gson.toJson(Map.of("error", e.getMessage()));
                    }

                    response.setContentType("application/json; charset=UTF-8");
                    PrintWriter printWriter = response.getWriter();
                    printWriter.println(json);
                    printWriter.close();
                }
            }
            return;
        }

        try {
            log.info("------------------------------------------------------");
            checkAuthorizationHeader(authHeader);   // header 가 올바른 형식인지 체크
            String accessToken = JwtUtils.getTokenFromHeader(authHeader);
            jwtUtils.validateToken(accessToken); // 토큰 검증
            jwtUtils.isTokenBlacklisted(authHeader); // 블랙리스트 확인 = 로그아웃된 회원인지 확인
            log.info("------------------------------------------------------");
        } catch (Exception e) {
            Gson gson = new Gson();
            String json = "";
            if (e instanceof CustomJwtException) {
                request.setAttribute("exceptionCode", ((CustomJwtException) e).getCode());
                request.setAttribute("exceptionMessage", e.getMessage());
                request.setAttribute("exceptionHttpStatus", ((CustomJwtException) e).getHttpStatus());
                throw new InsufficientAuthenticationException(e.getMessage()); // EntryPoint로 넘김
            } else {
                json = gson.toJson(Map.of("error", e.getMessage()));
            }

            response.setContentType("application/json; charset=UTF-8");
            PrintWriter printWriter = response.getWriter();
            printWriter.println(json);
            printWriter.close();

            return;
        }
        // accessToken != null 처리해주어야함.

        log.info("--------------------------- JwtVerifyFilter ---------------------------");

        try {
            checkAuthorizationHeader(authHeader);   // header 가 올바른 형식인지 체크
            String token = JwtUtils.getTokenFromHeader(authHeader);
            jwtUtils.validateToken(token); // 토큰 검증
//            jwtUtils.isExpired(token); // 토큰 만료 검증 <- 토큰 검증에서 수행

            Authentication authentication = jwtUtils.getAuthentication(token); // 사용자 인증 정보 생성
            log.info("authentication = {}", authentication);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);    // 다음 필터로 이동
        } catch (Exception e) {
            Gson gson = new Gson();
            String json = "";
            if (e instanceof CustomJwtException) {
                request.setAttribute("exceptionCode", ((CustomJwtException) e).getCode());
                request.setAttribute("exceptionMessage", e.getMessage());
                request.setAttribute("exceptionHttpStatus", ((CustomJwtException) e).getHttpStatus());
                throw new InsufficientAuthenticationException(e.getMessage()); // EntryPoint로 넘김
            } else {
                json = gson.toJson(Map.of("error", e.getMessage()));
            }

            response.setContentType("application/json; charset=UTF-8");
            PrintWriter printWriter = response.getWriter();
            printWriter.println(json);
            printWriter.close();
        }
    }
}
