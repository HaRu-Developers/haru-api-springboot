package com.haru.api.global.annotation;

import com.haru.api.domain.lastOpened.entity.enums.DocumentType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@AuthDocument(documentType = DocumentType.SNS_EVENT_ASSISTANT, pathVariableName = "snsEventId")
public @interface AuthSnsEvent {
}
