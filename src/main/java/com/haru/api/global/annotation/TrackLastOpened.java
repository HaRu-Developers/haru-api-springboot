package com.haru.api.global.annotation;

import com.haru.api.domain.lastOpened.entity.enums.DocumentType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TrackLastOpened {
    DocumentType type();
    int userIdIndex() default 0;
    int documentIdIndex() default 1;
}
