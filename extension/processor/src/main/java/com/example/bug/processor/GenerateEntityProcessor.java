package com.example.bug.processor;

import com.example.bug.runtime.GenerateEntity;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

/**
 * APT processor that generates a JPA @Entity class for each @GenerateEntity annotation.
 * The generated class is placed in the same package as the annotated class.
 */
@SupportedAnnotationTypes("com.example.bug.runtime.GenerateEntity")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class GenerateEntityProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(GenerateEntity.class)) {
            GenerateEntity ann = element.getAnnotation(GenerateEntity.class);
            String entityName = ann.value();
            String persistenceUnit = ann.persistenceUnit();

            PackageElement pkg = processingEnv.getElementUtils().getPackageOf(element);
            String packageName = pkg.getQualifiedName().toString();
            String className = entityName + "GeneratedEntity";
            String fqcn = packageName + "." + className;

            try {
                JavaFileObject file = processingEnv.getFiler().createSourceFile(fqcn, element);
                try (PrintWriter w = new PrintWriter(file.openWriter())) {
                    w.println("package " + packageName + ";");
                    w.println();
                    w.println("import com.example.bug.runtime.GeneratedEntityMarker;");
                    w.println("import jakarta.persistence.Entity;");
                    w.println("import jakarta.persistence.Id;");
                    w.println("import jakarta.persistence.Table;");
                    w.println("import java.util.UUID;");
                    w.println();
                    w.println("@Entity");
                    w.println("@Table(name = \"generated_entity\")");
                    w.println("@GeneratedEntityMarker(name = \"" + entityName
                            + "\", persistenceUnit = \"" + persistenceUnit + "\")");
                    w.println("public class " + className + " {");
                    w.println();
                    w.println("    @Id");
                    w.println("    private UUID id;");
                    w.println();
                    w.println("    private String payload;");
                    w.println();
                    w.println("    public UUID getId() { return id; }");
                    w.println("    public void setId(UUID id) { this.id = id; }");
                    w.println();
                    w.println("    public String getPayload() { return payload; }");
                    w.println("    public void setPayload(String payload) { this.payload = payload; }");
                    w.println("}");
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to generate entity " + fqcn, e);
            }
        }
        return true;
    }
}
