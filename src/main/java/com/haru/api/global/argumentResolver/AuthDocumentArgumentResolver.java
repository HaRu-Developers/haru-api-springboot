package com.haru.api.global.argumentResolver;

import com.haru.api.domain.lastOpened.entity.Documentable;
import com.haru.api.domain.lastOpened.entity.enums.DocumentType;
import com.haru.api.global.annotation.AuthDocument;
import com.haru.api.global.documentFinder.DocumentFinder;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AuthDocumentArgumentResolver implements HandlerMethodArgumentResolver {

    private final Map<DocumentType, DocumentFinder> finders;

    public AuthDocumentArgumentResolver(List<DocumentFinder> finderList) {
        this.finders = finderList.stream()
                .collect(Collectors.toMap(DocumentFinder::getSupportType, Function.identity()));
    }

    // 어떤 파라미터를 해당 Resolver가 처리할지 결정하는 메서드
    @Override
    public boolean supportsParameter(MethodParameter parameter) {

        // 파라미터에 @AuthDocument 계열 어노테이션이 있는지 확인
        AuthDocument authDocumentInfo = findAuthDocumentAnnotation(parameter);

        if (authDocumentInfo != null) {
            // 파라미터 타입이 Documentable 인터페이스를 구현했는지 확인
            Class<?> parameterType = parameter.getParameterType();
            return Documentable.class.isAssignableFrom(parameterType);
        }

        return false;
    }

    // supportsParameter가 true일 때, 파라미터에 주입할 객체를 반환
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

        Object isValidated = webRequest.getAttribute("isValidated", NativeWebRequest.SCOPE_REQUEST);

        if (isValidated instanceof Boolean) {

            // 이미 interceptor에서 검증된 document 경우, 넘겨받은 document 반환
            return webRequest.getAttribute("validatedDocument", NativeWebRequest.SCOPE_REQUEST);

        } else {
            // 파라미터의 어노테이션에서 @AuthDocument 어노테이션에서 정보 추출
            AuthDocument authDocument = findAuthDocumentAnnotation(parameter);
            DocumentType documentType = authDocument.documentType();
            String pathVariableName = authDocument.pathVariableName();

            // 요청 URL에서 path variable 추출
            Map<String, String> pathVariables = (Map<String, String>) webRequest.getAttribute(
                    HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST);

            String idString = pathVariables.get(pathVariableName);
            if (idString == null) {
                throw new IllegalStateException("경로 변수 '" + pathVariableName + "'를 찾을 수 없습니다.");
            }
            Long id = Long.parseLong(idString);

            // DocumentType에 맞는 finder를 찾아 객체를 조회
            DocumentFinder finder = finders.get(documentType);
            if (finder == null)
                throw new IllegalStateException(documentType + " 타입을 처리할 Finder가 없습니다.");

            return finder.findById(id);
        }
    }

    private AuthDocument findAuthDocumentAnnotation(MethodParameter parameter) {
        for (Annotation annotation : parameter.getParameterAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(AuthDocument.class)) {
                return annotation.annotationType().getAnnotation(AuthDocument.class);
            }
        }
        return null;
    }
}
