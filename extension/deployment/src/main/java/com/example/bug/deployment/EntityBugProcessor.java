package com.example.bug.deployment;

import com.example.bug.runtime.GeneratedEntityMarker;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalJpaModelBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import java.util.Collection;
import java.util.Set;

/**
 * Discovers APT-generated entity classes via @GeneratedEntityMarker in Jandex
 * and registers them via AdditionalJpaModelBuildItem.
 * <p>
 * This causes NPE in HibernateOrmProcessor.enhanceEntities() at:
 * <pre>
 *   byte[] bytes = IoUtil.readClassAsBytes(HibernateOrmProcessor.class.getClassLoader(), className);
 * </pre>
 * The deployment classloader (HibernateOrmProcessor.class.getClassLoader()) cannot see
 * application-module classes — both APT-generated and manually written.
 * The returned bytes are null, causing NPE downstream.
 */
class EntityBugProcessor {

    private static final String FEATURE = "entity-bug";
    private static final DotName GENERATED_ENTITY_MARKER = DotName.createSimple(GeneratedEntityMarker.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerGeneratedEntities(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<AdditionalJpaModelBuildItem> additionalJpaModel) {

        IndexView index = combinedIndex.getIndex();
        Collection<AnnotationInstance> annotations = index.getAnnotations(GENERATED_ENTITY_MARKER);
        if (annotations.isEmpty()) {
            return;
        }

        for (AnnotationInstance annotation : annotations) {
            String entityClass = annotation.target().asClass().name().toString();

            AnnotationValue puValue = annotation.value("persistenceUnit");
            String puName = (puValue != null && !puValue.asString().isEmpty())
                    ? puValue.asString()
                    : "<default>";

            System.out.printf("[entity-bug] Registering %s in PU '%s' via AdditionalJpaModelBuildItem%n",
                    entityClass, puName);

            additionalJpaModel.produce(new AdditionalJpaModelBuildItem(entityClass, Set.of(puName)));
        }
    }
}
