package com.hariklabs.usermanager03.service;

import com.hariklabs.usermanager03.persistence.UserEntity;
import com.hariklabs.usermanager03.persistence.UserRepository;
import com.hariklabs.usermanager03.persistence.impl.JpaUserStore;
import com.hariklabs.usermanager03.rest.UsersController;
import com.hariklabs.usermanager03.service.impl.UserServiceImpl;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureTest {
    private static final String BASE = "com.hariklabs.usermanager03.";
    private static final String SERVICE = BASE + "service";
    private static final String REST = BASE + "rest";
    private static final String PERSISTENCE = BASE + "persistence";
    private static final String PERSISTENCE_API = PERSISTENCE + ".api";

    private static final ArchRule SERVICE_BOUNDARY = classes()
            .that().resideInAPackage(SERVICE + "..")
            .should().onlyDependOnClassesThat(new DescribedPredicate<>(
                    "outside REST, JPA, Spring Data, PostgreSQL and persistence internals") {
                @Override
                public boolean test(JavaClass target) {
                    String packageName = target.getPackageName();
                    return !inLayer(packageName, REST)
                            && !inLayer(packageName, "jakarta.persistence")
                            && !inLayer(packageName, "javax.persistence")
                            && !inLayer(packageName, "org.hibernate")
                            && !inLayer(packageName, "org.springframework.data")
                            && !inLayer(packageName, "org.postgresql")
                            && (!inLayer(packageName, PERSISTENCE)
                                    || inLayer(packageName, PERSISTENCE_API));
                }
            });

    private static final ArchRule REST_BOUNDARY = classes()
            .that().resideInAPackage(REST + "..")
            .should().onlyDependOnClassesThat(notInLayer(PERSISTENCE));

    private static final ArchRule PERSISTENCE_BOUNDARY = classes()
            .that().resideInAPackage(PERSISTENCE + "..")
            .should().onlyDependOnClassesThat(notInLayer(REST));

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages(BASE.substring(0, BASE.length() - 1));

    @Test
    void serviceDependsOnPersistenceOnlyThroughItsApi() {
        assertRepresentativeClasses();
        assertHasSubjects(PRODUCTION_CLASSES, SERVICE);
        SERVICE_BOUNDARY.check(PRODUCTION_CLASSES);
    }

    @Test
    void restDoesNotDependOnPersistence() {
        assertRepresentativeClasses();
        assertHasSubjects(PRODUCTION_CLASSES, REST);
        REST_BOUNDARY.check(PRODUCTION_CLASSES);
    }

    @Test
    void persistenceDoesNotDependOnRest() {
        assertRepresentativeClasses();
        assertHasSubjects(PRODUCTION_CLASSES, PERSISTENCE);
        PERSISTENCE_BOUNDARY.check(PRODUCTION_CLASSES);
    }

    @Test
    void serviceRuleDetectsDirectJpaDependency() {
        JavaClasses invalidServiceOnly = new ClassFileImporter().importClasses(InvalidService.class);
        assertHasSubjects(invalidServiceOnly, SERVICE);
        EvaluationResult result = SERVICE_BOUNDARY.evaluate(invalidServiceOnly);
        String report = result.getFailureReport().toString();
        assertTrue(result.hasViolation(), report);
        assertTrue(report.contains("EntityManager") && report.contains("jakarta.persistence"), report);
    }

    private static DescribedPredicate<JavaClass> notInLayer(String layer) {
        return new DescribedPredicate<>("outside " + layer) {
            @Override
            public boolean test(JavaClass target) {
                return !inLayer(target.getPackageName(), layer);
            }
        };
    }

    private static boolean inLayer(String packageName, String layer) {
        return packageName.equals(layer) || packageName.startsWith(layer + ".");
    }

    private static void assertHasSubjects(JavaClasses classes, String layer) {
        assertTrue(classes.stream().anyMatch(javaClass -> inLayer(javaClass.getPackageName(), layer)),
                "No compiled classes found in " + layer);
    }

    private static void assertRepresentativeClasses() {
        assertContains(PRODUCTION_CLASSES, UserServiceImpl.class);
        assertContains(PRODUCTION_CLASSES, UsersController.class);
        assertContains(PRODUCTION_CLASSES, UserEntity.class);
        assertContains(PRODUCTION_CLASSES, UserRepository.class);
        assertContains(PRODUCTION_CLASSES, JpaUserStore.class);
        assertFalse(contains(PRODUCTION_CLASSES, InvalidService.class));
    }

    private static void assertContains(JavaClasses classes, Class<?> type) {
        assertTrue(contains(classes, type), "Compiled class missing: " + type.getName());
    }

    private static boolean contains(JavaClasses classes, Class<?> type) {
        return classes.stream().anyMatch(javaClass -> javaClass.getName().equals(type.getName()));
    }

    static final class InvalidService {
        private EntityManager entityManager;
    }
}
