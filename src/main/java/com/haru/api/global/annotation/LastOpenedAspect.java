package com.haru.api.global.annotation;

import com.haru.api.domain.lastOpened.entity.enums.DocumentType;
import com.haru.api.domain.lastOpened.service.UserDocumentLastOpenedService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Order(1)
public class LastOpenedAspect {

    private final UserDocumentLastOpenedService userDocumentLastOpenedService;

    @Around("@annotation(trackLastOpened)")
    public Object trackLastOpened(ProceedingJoinPoint joinPoint, TrackLastOpened trackLastOpened) throws Throwable {
        // 실제 메서드 실행
        Object result = joinPoint.proceed();

        DocumentType type = trackLastOpened.type();
        int userIdIndex = trackLastOpened.userIdIndex();
        int documentIdIndex = trackLastOpened.documentIdIndex();

        // 메서드의 인자에서 userId와 documentId 추출
        Object[] args = joinPoint.getArgs();

        // 인덱스를 사용하여 userId와 documentId 추출
        Long userId = (Long) args[userIdIndex];
        Long documentId = (Long) args[documentIdIndex];

        if (userId != null && documentId != null) {
            userDocumentLastOpenedService.updateLastOpened(userId, type, documentId);
        }

        return result;
    }

}
