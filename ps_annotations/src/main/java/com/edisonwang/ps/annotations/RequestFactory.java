package com.edisonwang.ps.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author edi
 */
@Target(value = ElementType.TYPE)
@Retention(value = RetentionPolicy.CLASS)
@Inherited
public @interface RequestFactory {

    Class baseClass() default Object.class; //Action.java

    Class valueType() default Object.class; //ActionKey.java

    String group();
}
