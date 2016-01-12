package com.edisonwang.eventservice.annotations;

/**
 * @author edi
 */
public @interface ParcelableClassField {
    String name();
    Class kind();
    Class bagger();
}