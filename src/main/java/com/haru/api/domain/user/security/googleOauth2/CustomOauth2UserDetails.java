package com.haru.api.domain.user.security.googleOauth2;

import com.haru.api.domain.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class CustomOauth2UserDetails implements OAuth2User { // 구글 OAuth2 인증 후 사용자 정보를 담는 클래스

    private final User user;
    private Map<String, Object> attributes;
    private boolean isNewUser;

    public CustomOauth2UserDetails(User user, Map<String, Object> attributes, boolean isNewUser) {
        this.user = user;
        this.attributes = attributes;
        this.isNewUser = isNewUser;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return (String) attributes.get("sub");
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList(); // 권한 사용x
    }

    public User getUser() {
        return user;
    }

    public boolean getIsNewUser() {
        return isNewUser;
    }
}
