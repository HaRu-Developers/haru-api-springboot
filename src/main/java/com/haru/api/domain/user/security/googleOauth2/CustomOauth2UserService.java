package com.haru.api.domain.user.security.googleOauth2;

import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.user.entity.enums.Status;
import com.haru.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOauth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException { // 구글 OAuth서버와 Resource서버에서 유저 정보를 가져오는 메서드
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String providerId = (String) oAuth2User.getAttributes().get("sub");
        String email = (String) oAuth2User.getAttributes().get("email");
        String name = (String) oAuth2User.getAttributes().get("name");
        String profileImage = (String) oAuth2User.getAttributes().get("picture");
        User foundUser = userRepository.findByProviderId(providerId).orElse(null);
        if (foundUser == null) {
            User newUser = User.builder()
                    .name(name)
                    .email(email)
                    .status(Status.ACTIVE)
                    .profileImage(profileImage)
                    .providerId(providerId)
                    .build();
            return new CustomOauth2UserDetails(userRepository.save(newUser), oAuth2User.getAttributes(), true);
        } else{
            return new CustomOauth2UserDetails(foundUser, oAuth2User.getAttributes(), false);
        }
    }
}
