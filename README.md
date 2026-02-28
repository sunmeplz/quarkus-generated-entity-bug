# AdditionalJpaModelBuildItem NPE for application-classpath entities

## Bug summary

`HibernateOrmProcessor#enhancerDomainObjects` throws `NullPointerException` when an extension registers an application-module entity via `AdditionalJpaModelBuildItem`.

The root cause is in `enhanceEntities()` ([HibernateOrmProcessor.java:1169](https://github.com/quarkusio/quarkus/blob/3.30.6/extensions/hibernate-orm/deployment/src/main/java/io/quarkus/hibernate/orm/deployment/HibernateOrmProcessor.java#L1169)):

```java
byte[] bytes = IoUtil.readClassAsBytes(HibernateOrmProcessor.class.getClassLoader(), className);
```

`HibernateOrmProcessor.class.getClassLoader()` is the **deployment classloader**, which cannot see application-module classes. `readClassAsBytes()` returns `null`, causing NPE downstream at `ModelTypePool.registerClassNameAndBytes()`.

This affects **any** application-module entity registered via `AdditionalJpaModelBuildItem` — both APT-generated and manually written.

## Quarkus version

3.30.6

## Use case

An extension uses an APT annotation processor to generate JPA `@Entity` classes at compile time in the application module (e.g., outbox entities for saga patterns). The extension's deployment processor discovers these generated entities via Jandex and registers them with `AdditionalJpaModelBuildItem` to assign them to specific persistence units.

This pattern is necessary because:
- The generated entity class names are dynamic (per-adapter/per-connector)
- Multi-PU setups require explicit entity-to-PU assignment
- `.packages` config shouldn't be required for framework-generated entities

## How to reproduce

```bash
mvn clean install -DskipTests
```

**Expected:** Build succeeds, the generated entity is registered in the default PU.

**Actual:** NPE during `quarkus:build` (packaging phase):

```
Build step io.quarkus.hibernate.orm.deployment.HibernateOrmProcessor#enhancerDomainObjects
  threw an exception: java.lang.NullPointerException
    at ModelTypePool.registerClassNameAndBytes(ModelTypePool.java:88)
    at EnhancerImpl.enhance(EnhancerImpl.java:133)
    at HibernateEntityEnhancer.enhance(HibernateEntityEnhancer.java:94)
    at HibernateOrmProcessor.enhanceEntities(HibernateOrmProcessor.java:1171)
    at HibernateOrmProcessor.enhancerDomainObjects(HibernateOrmProcessor.java:676)
```

Note: tests pass (`mvn test` works) because the test classloader has visibility to application classes. The NPE only occurs during packaging (`quarkus:build`).

## Project structure

```
quarkus-generated-entity-bug/
├── extension/
│   ├── runtime/       # @GenerateEntity and @GeneratedEntityMarker annotations
│   ├── processor/     # APT processor — generates a JPA @Entity class
│   └── deployment/    # Quarkus build step — registers entity via AdditionalJpaModelBuildItem
└── app/               # Application — uses @GenerateEntity, triggers the bug
```

### Flow

1. `app/MyService.java` is annotated with `@GenerateEntity("MyTest")`
2. APT processor generates `MyTestGeneratedEntity.java` (a JPA `@Entity`) in `app/target/generated-sources/`
3. Jandex indexes the generated class (via `@GeneratedEntityMarker`)
4. Extension deployment processor discovers it and calls:
   ```java
   additionalJpaModel.produce(new AdditionalJpaModelBuildItem(entityClass, Set.of("<default>")));
   ```
5. `HibernateOrmProcessor.enhanceEntities()` tries to load the entity bytecode via the deployment classloader → `null` → NPE

## Suggested fix

`enhanceEntities()` should use a classloader that can see application-module classes (e.g., the runtime/application classloader or the Quarkus build classloader) when loading bytecode for entities registered via `AdditionalJpaModelBuildItem`, rather than `HibernateOrmProcessor.class.getClassLoader()`.

Alternatively, `AdditionalJpaModelBuildItem` documentation should clarify that it only works for entities on the deployment classpath (i.e., in extension runtime JARs), not for application-module entities.
