package com.haru.api.global.argumentResolver;

import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.user.repository.UserRepository;
import com.haru.api.domain.user.security.jwt.SecurityUtil;
import com.haru.api.global.annotation.AuthUser;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.MemberHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class AuthUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserRepository userRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthUser.class) &&
                parameter.getParameterType().equals(User.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

        Object isValidated = webRequest.getAttribute("isValidated", NativeWebRequest.SCOPE_REQUEST);

        if (isValidated instanceof Boolean) {
            // 이미 interceptor에서 검증된 유저인 경우, 넘겨받은 user 반환
            return webRequest.getAttribute("validatedUser", NativeWebRequest.SCOPE_REQUEST);
        } else {
            // 현재 로그인 해 있는 유저 ID 반환
            Long userId = SecurityUtil.getCurrentUserId();

            // 해당 유저가 존재하는지 확인하고, 존재하면 해당 user 객체 반환
            return userRepository.findById(userId)
                    .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));
        }
    }

}
