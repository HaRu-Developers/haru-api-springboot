package com.haru.api.global.annotation;

import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.domain.workspace.repository.WorkspaceRepository;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.WorkspaceHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthWorkspaceArgumentResolver implements HandlerMethodArgumentResolver {

    private final WorkspaceRepository workspaceRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthWorkspace.class) &&
                parameter.getParameterType().equals(Workspace.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

        final HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);

        // URL pathVariable에서 workspaceId 추출
        final Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        if (pathVariables == null) throw new RuntimeException("empty path variables");

        final String workspaceId = pathVariables.get("workspaceId");

        // workspace 존재하는지 확인하고, 존재한다면 해당 workspace 객체 반환
        return workspaceRepository.findById(Long.parseLong(workspaceId))
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));
    }
}
