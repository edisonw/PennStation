package com.edisonwang.ps.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author edi
 */
@Retention(value = RetentionPolicy.CLASS)
public @interface ClassField {
    String name();

    Class kind();

    boolean required() default false;
}