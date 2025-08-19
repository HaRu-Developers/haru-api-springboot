package com.haru.api.global.annotation;

import com.haru.api.domain.lastOpened.entity.enums.DocumentType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@AuthDocument(documentType = DocumentType.TEAM_MOOD_TRACKER, pathVariableName = "mood-tracker-hashed-Id")
public @interface AuthMoodTracker {
}
