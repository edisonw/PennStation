package com.edisonwang.eventservice.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Event listener will generate a list of events based on the producer it was specified for.
 *
 * @author edi
 */
@Target(value = ElementType.TYPE)
@Retention(value = RetentionPolicy.CLASS)
@Inherited
public @interface EventListener {
    boolean restrictMainThread() default true;
    Class[] producers();
}
