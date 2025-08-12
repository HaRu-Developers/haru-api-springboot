package com.haru.api.global.config;

import com.haru.api.global.annotation.AuthUserArgumentResolver;
import com.haru.api.global.annotation.AuthWorkspaceArgumentResolver;
import com.haru.api.global.interceptor.WorkspaceMemberAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthUserArgumentResolver authUserArgumentResolver;
    private final AuthWorkspaceArgumentResolver authWorkspaceArgumentResolver;
    private final WorkspaceMemberAuthInterceptor workspaceMemberAuthInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:3000", "https://haru.it.kr", "https://api.haru.it.kr", "*") // 프론트엔드 주소 추가
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(authUserArgumentResolver);
        argumentResolvers.add(authWorkspaceArgumentResolver);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(workspaceMemberAuthInterceptor);
    }
}
