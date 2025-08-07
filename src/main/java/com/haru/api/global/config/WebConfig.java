package com.haru.api.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:3000", "https://haru.it.kr", "https://api.haru.it.kr", "*") // 프론트엔드 주소 추가
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
