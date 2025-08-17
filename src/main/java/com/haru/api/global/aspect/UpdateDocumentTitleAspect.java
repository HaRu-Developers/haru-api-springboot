package com.haru.api.global.aspect;

import com.haru.api.domain.lastOpened.entity.Documentable;
import com.haru.api.domain.lastOpened.service.UserDocumentLastOpenedService;
import com.haru.api.global.common.entity.TitleHolder;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class UpdateDocumentTitleAspect {

    private final UserDocumentLastOpenedService userDocumentLastOpenedService;

    @AfterReturning("@annotation(com.haru.api.global.annotation.UpdateDocumentTitle)")
    public void afterTitleUpdate(JoinPoint joinPoint) {

        Documentable document = null;
        TitleHolder titleHolder = null;

        // 실행된 메서드의 인자 추출
        Object[] args = joinPoint.getArgs();

        // 인자들 중에서 Documentable 타입의 객체 추출
        for (Object arg : args) {
            if (arg instanceof Documentable) {
                document = (Documentable) arg;
            } else if (arg instanceof TitleHolder) {
                titleHolder = (TitleHolder) arg;
            }
        }

        // document, titleHolder가 정상적으로 조회되면 last opened 테이블에서 해당 문서의 제목 수정
        if (document != null && titleHolder != null) {
            userDocumentLastOpenedService.updateRecordsForWorkspaceUsers(document, titleHolder);
        }
    }
}
