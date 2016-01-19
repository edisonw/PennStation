package com.edisonwang.ps.annotations;

/**
 * @author edi
 */
public @interface ParcelableClassField {
    String name();
    Class kind();
    Class parceler() default Object.class;
    boolean required() default true;
}