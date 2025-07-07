package com.haru.api.domain.user.security;

import com.haru.api.domain.user.entity.Users;
import com.haru.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements CustomDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email, String password) throws UsernameNotFoundException {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("해당 아이디를 가진 유저가 존재하지 않습니다: " + email));

        User securityUser = new User(
                user.getEmail(),
                user.getPassword(),
                Collections.emptyList()
        );

        return securityUser;
    }
}
