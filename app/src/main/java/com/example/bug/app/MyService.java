package com.example.bug.app;

import com.example.bug.runtime.GenerateEntity;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Application class annotated with @GenerateEntity.
 * APT will generate MyTestGeneratedEntity.java in this package.
 * The extension deployment processor will register it via AdditionalJpaModelBuildItem.
 * This triggers NPE during quarkus:build (packaging).
 */
@ApplicationScoped
@GenerateEntity("MyTest")
public class MyService {
}
