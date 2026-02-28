package com.example.bug.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker placed on APT-generated entity classes so the deployment
 * processor can discover them via Jandex.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GeneratedEntityMarker {

    /** The entity name from @GenerateEntity. */
    String name();

    /** The persistence unit from @GenerateEntity. */
    String persistenceUnit() default "";
}
