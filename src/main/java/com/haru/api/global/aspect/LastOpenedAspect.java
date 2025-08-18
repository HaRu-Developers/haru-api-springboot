package com.haru.api.global.aspect;

import com.haru.api.domain.lastOpened.entity.Documentable;
import com.haru.api.domain.lastOpened.entity.UserDocumentId;
import com.haru.api.domain.lastOpened.entity.enums.DocumentType;
import com.haru.api.domain.lastOpened.service.UserDocumentLastOpenedService;
import com.haru.api.domain.user.entity.User;
import com.haru.api.global.annotation.TrackLastOpened;
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

        // 메서드의 인자에서 user와 document 추출
        Object[] args = joinPoint.getArgs();

        // 인덱스를 사용하여 user와 document 추출
        User user = (User)args[0];
        Documentable document = (Documentable)args[1];

        if (user != null && document != null) {

            Long workspaceId = document.getWorkspaceId();
            String title = document.getTitle();

            UserDocumentId userDocumentId = new UserDocumentId(user.getId(), document.getId(), document.getDocumentType());

            userDocumentLastOpenedService.updateLastOpened(userDocumentId, workspaceId, title);
        }

        return result;
    }

}
