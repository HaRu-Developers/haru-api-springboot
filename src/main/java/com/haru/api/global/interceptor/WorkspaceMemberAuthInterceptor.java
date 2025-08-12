package com.haru.api.global.interceptor;

import com.haru.api.domain.user.security.jwt.SecurityUtil;
import com.haru.api.domain.userWorkspace.repository.UserWorkspaceRepository;
import com.haru.api.global.annotation.AuthUser;
import com.haru.api.global.annotation.AuthWorkspace;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.UserWorkspaceHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WorkspaceMemberAuthInterceptor implements HandlerInterceptor {

    private final UserWorkspaceRepository userWorkspaceRepository;

    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) throws Exception {

        // 컨트롤러 메서드인지 확인
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        final HandlerMethod handlerMethod = (HandlerMethod) handler;

        // @AuthUser, @AuthWorkspace 어노테이션이 달린 인자가 있는지 확인
        boolean hasAuthUserParam = false;
        boolean hasAuthWorkspaceParam = false;

        for (final var param : handlerMethod.getMethodParameters()) {
            if (param.hasParameterAnnotation(AuthUser.class)) {
                hasAuthUserParam = true;
            }
            if (param.hasParameterAnnotation(AuthWorkspace.class)) {
                hasAuthWorkspaceParam = true;
            }
        }

        // 컨트롤러에 AuthUser, AuthWorkspace 어노테이션이 모두 달린 경우에 해당
        if(hasAuthUserParam && hasAuthWorkspaceParam) {

            // 1. URL pathVariable에서 workspaceId 추출
            final Map<String, String> pathVariables =
                    (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            final String workspaceId = pathVariables.get("workspaceId");

            if (workspaceId == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Workspace ID is missing from path.");
                return false;
            }

            // 2. Security Context에서 현재 유저 ID 추출
            final Long userId = SecurityUtil.getCurrentUserId();

            // 3. 유저가 워크스페이스에 속하는지 확인
            final boolean isUserInWorkspace = userWorkspaceRepository.existsByUserIdAndWorkspaceId(userId, Long.parseLong(workspaceId));

            // 4. 속하지 않는 경우 예외 발생
            if(!isUserInWorkspace) throw new UserWorkspaceHandler(ErrorStatus.USER_WORKSPACE_NOT_FOUND);
        }

        return true;
    }
}
