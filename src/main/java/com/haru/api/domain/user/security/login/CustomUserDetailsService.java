package com.haru.api.domain.user.security.login;

import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements CustomDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email, String password) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("해당 아이디를 가진 유저가 존재하지 않습니다: " + email));

        org.springframework.security.core.userdetails.User securityUser = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.emptyList()
        );

        return securityUser;
    }
}
