package com.haru.api.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${api-url.fast-api}")
    private String fastApiUrl;

    @Bean
    public WebClient fastApiWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(fastApiUrl)
                .build();
    }
}
