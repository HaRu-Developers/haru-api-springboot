package com.haru.api.global.aspect;

import com.haru.api.domain.lastOpened.entity.Documentable;
import com.haru.api.domain.lastOpened.service.UserDocumentLastOpenedService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class DeleteDocumentAspect {

    private final UserDocumentLastOpenedService userDocumentLastOpenedService;

    @AfterReturning("@annotation(com.haru.api.global.annotation.DeleteDocument)")
    public void afterDeleteDocument(JoinPoint joinPoint) {

        // 실행된 메서드의 인자 추출
        Object[] args = joinPoint.getArgs();

        Documentable document = null;

        // 인자들 중에서 Documentable 타입의 객체 추출
        for (Object arg : args) {
            if (arg instanceof Documentable) {
                document = (Documentable) arg;
                break;
            }
        }

        // Documentable 객체를 토대로 서비스 로직 호출
        if (document != null) {
            userDocumentLastOpenedService.deleteRecordsForWorkspaceUsers(document);
        }
    }
}
