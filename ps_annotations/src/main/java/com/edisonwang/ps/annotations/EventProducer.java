package com.edisonwang.ps.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Event producer generates a list of events it can produce.
 *
 * @author edi
 */
@Target(value = ElementType.TYPE)
@Retention(value = RetentionPolicy.CLASS)
@Inherited
public @interface EventProducer {
    /**
     * @return Event classes that needs to be generated.
     */
    Event[] generated() default {};

    Class[] events() default {};
}
