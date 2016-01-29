package com.edisonwang.ps.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author edi
 */
@Retention(value = RetentionPolicy.CLASS)
public @interface ParcelableClassField {
    String name();

    Class kind();

    Class parceler() default Default.class;

    boolean required() default true;
}