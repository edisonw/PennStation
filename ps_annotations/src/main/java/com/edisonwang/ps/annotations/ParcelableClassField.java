package com.edisonwang.ps.annotations;

/**
 * @author edi
 */
public @interface ParcelableClassField {
    String name();
    Class kind();
    Class parceler();
    boolean required() default true;
}