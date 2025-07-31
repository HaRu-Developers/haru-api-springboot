package com.haru.api.domain.user.security;

import com.haru.api.global.apiPayload.exception.handler.MemberHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import static com.haru.api.global.apiPayload.code.status.ErrorStatus.MEMBER_PASSWORD_NOT_MATCH;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final CustomDetailsService customDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {
        UserDetails userDetails = customDetailsService.loadUserByUsername((String) authentication.getPrincipal(), (String) authentication.getCredentials());

        String password = authentication.getCredentials().toString();
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new MemberHandler(MEMBER_PASSWORD_NOT_MATCH);
        }

        return new UsernamePasswordAuthenticationToken(userDetails, authentication.getCredentials(), userDetails.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}