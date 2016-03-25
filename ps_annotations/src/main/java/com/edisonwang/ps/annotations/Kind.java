package com.edisonwang.ps.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = ElementType.ANNOTATION_TYPE)
@Retention(value = RetentionPolicy.CLASS)
@Inherited
public @interface Kind {
    Class clazz();
    Class parameter() default Default.class;
}
