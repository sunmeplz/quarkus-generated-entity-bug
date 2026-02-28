package com.example.bug.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Triggers APT generation of a JPA @Entity class in the same package.
 * The generated entity is then registered via AdditionalJpaModelBuildItem
 * by the extension's deployment processor.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GenerateEntity {

    /** Name for the generated entity class. */
    String value();

    /** Persistence unit name. Empty means default PU. */
    String persistenceUnit() default "";
}
