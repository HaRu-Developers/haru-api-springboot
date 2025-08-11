package com.haru.api.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${api.stt-api-url}")
    private String sttApiUrl;

    @Value("${api.score-api-url}")
    private String scoreApiUrl;

    @Value("${api.openai.api-key}")
    private String openaiApiKey;

    @Bean
    public WebClient fastApiWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(sttApiUrl)
                .build();
    }

    @Bean
    public WebClient scoreApiWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(scoreApiUrl)
                .build();
    }

    @Bean
    public WebClient chatGPTWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("https://api.openai.com/v1/chat/completions")
                .defaultHeader("Authorization", "Bearer " + openaiApiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
