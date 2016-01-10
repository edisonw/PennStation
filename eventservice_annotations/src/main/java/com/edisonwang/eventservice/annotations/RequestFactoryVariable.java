package com.edisonwang.eventservice.annotations;

/**
 * @author edi
 */
public @interface RequestFactoryVariable {
    String name();
    Class kind();
}