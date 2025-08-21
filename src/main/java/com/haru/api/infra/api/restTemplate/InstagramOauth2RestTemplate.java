package com.haru.api.infra.api.restTemplate;

import com.haru.api.domain.snsEvent.entity.enums.InstagramRedirectType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class InstagramOauth2RestTemplate {
    @Value("${instagram.client.id}")
    private String instagramClientId;
    @Value("${instagram.client.secret}")
    private String instagramClientSecret;
    @Value("${instagram.redirect.uri-onboarding}")
    private String instagramRedirectUriOnboarding;
    @Value("${instagram.redirect.uri-workspace}")
    private String instagramRedirectUriWorkspace;

    private final RestTemplate restTemplate;

    public String getShortLivedAccessTokenUrl(
            String code,
            InstagramRedirectType instagramRedirectType
    ) {
        // 1. Access Token 요청
        String tokenUrl = "https://api.instagram.com/oauth/access_token";
        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", instagramClientId);          // 인스타 앱의 클라이언트 ID
        params.add("client_secret", instagramClientSecret);  // 인스타 앱의 클라이언트 시크릿
        params.add("grant_type", "authorization_code");
        // 인가코드와 동일한 redirect_uri
        if (instagramRedirectType == InstagramRedirectType.ONBOARDING) {
            params.add("redirect_uri", instagramRedirectUriOnboarding);
        } else if(instagramRedirectType == InstagramRedirectType.WORKSPACE) {
            System.out.println("Using workspace redirect URI: " + instagramRedirectUriWorkspace);
            params.add("redirect_uri", instagramRedirectUriWorkspace);
        }
        params.add("code", code);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(tokenUrl, request, Map.class);
        return (String) tokenResponse.getBody().get("access_token");
    }

    public String getLongLivedAccessToken(String shortLivedAccessToken) {
        // 2. 단기 토큰을 장기(Long-Lived) 토큰으로 교환
        String longLivedTokenUrl = "https://graph.instagram.com/access_token";

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(longLivedTokenUrl)
                .queryParam("grant_type", "ig_exchange_token")
                .queryParam("client_secret", instagramClientSecret)
                .queryParam("access_token", shortLivedAccessToken);

        ResponseEntity<Map> longLivedTokenResponse = restTemplate.getForEntity(builder.toUriString(), Map.class);
        Map<String, Object> longLivedTokenBody = longLivedTokenResponse.getBody();

        String longLivedAccessToken = (String) longLivedTokenBody.get("access_token");
        Integer expiresIn = (Integer) longLivedTokenBody.get("expires_in");

        System.out.println("Long-Lived Access Token: " + longLivedAccessToken);
        System.out.println("Expires in (seconds): " + expiresIn);

        return longLivedAccessToken;
    }

    public Map<String, Object> getInstagramAccountInfo(String longLivedAccessToken) {
        // 3. 장기 토큰으로 사용자 계정 정보 요청
        String userInfoUrl = UriComponentsBuilder.fromHttpUrl("https://graph.instagram.com/me")
                .queryParam("fields", "user_id,username")
                .queryParam("access_token", longLivedAccessToken)
                .toUriString();

        ResponseEntity<Map> userInfoResponse = restTemplate.getForEntity(userInfoUrl, Map.class);
        Map<String, Object> userInfo = userInfoResponse.getBody();

        System.out.println("Account Info: " + userInfo);

        // 결과 출력 (또는 필요한 로직)
        System.out.println("Access Token: " + longLivedAccessToken);
        System.out.println("Account Info: " + userInfo);

        return userInfo;
    }
}
