package com.edisonwang.ps.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author edi
 */
@Target(value = ElementType.ANNOTATION_TYPE)
@Retention(value = RetentionPolicy.CLASS)
public @interface ParcelableClassField {
    String name();

    Kind kind();

    Class parceler() default Default.class;

    boolean required() default true;
}