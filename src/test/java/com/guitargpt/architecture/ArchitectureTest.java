package com.guitargpt.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.guitargpt");
    }

    @Nested
    @DisplayName("Layer dependency rules")
    class LayerDependencyRules {

        @Test
        @DisplayName("Layered architecture is respected")
        void layeredArchitectureIsRespected() {
            layeredArchitecture()
                    .consideringAllDependencies()
                    .layer("Domain").definedBy("com.guitargpt.domain..")
                    .layer("Application").definedBy("com.guitargpt.application..")
                    .layer("Infrastructure").definedBy("com.guitargpt.infrastructure..")
                    .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure")
                    .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
                    .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
                    .check(classes);
        }

        @Test
        @DisplayName("Domain must not depend on Application")
        void domainMustNotDependOnApplication() {
            noClasses()
                    .that().resideInAPackage("com.guitargpt.domain..")
                    .should().dependOnClassesThat().resideInAPackage("com.guitargpt.application..")
                    .because("Domain must not know about Application layer")
                    .check(classes);
        }

        @Test
        @DisplayName("Domain must not depend on Infrastructure")
        void domainMustNotDependOnInfrastructure() {
            noClasses()
                    .that().resideInAPackage("com.guitargpt.domain..")
                    .should().dependOnClassesThat().resideInAPackage("com.guitargpt.infrastructure..")
                    .because("Domain must not know about Infrastructure layer")
                    .check(classes);
        }

        @Test
        @DisplayName("Application must not depend on Infrastructure")
        void applicationMustNotDependOnInfrastructure() {
            noClasses()
                    .that().resideInAPackage("com.guitargpt.application..")
                    .should().dependOnClassesThat().resideInAPackage("com.guitargpt.infrastructure..")
                    .because("Application must not know about Infrastructure layer")
                    .check(classes);
        }
    }

    @Nested
    @DisplayName("Domain isolation rules")
    class DomainIsolationRules {

        @Test
        @DisplayName("Domain must not use Spring framework")
        void domainMustNotUseSpring() {
            noClasses()
                    .that().resideInAPackage("com.guitargpt.domain..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                    .because("Domain must be framework-free")
                    .check(classes);
        }

        @Test
        @DisplayName("Domain must not use JPA/Hibernate")
        void domainMustNotUseJpa() {
            noClasses()
                    .that().resideInAPackage("com.guitargpt.domain..")
                    .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
                    .because("Domain must not depend on JPA")
                    .check(classes);
        }

        @Test
        @DisplayName("Domain must not use Kafka")
        void domainMustNotUseKafka() {
            noClasses()
                    .that().resideInAPackage("com.guitargpt.domain..")
                    .should().dependOnClassesThat().resideInAPackage("org.apache.kafka..")
                    .because("Domain must not depend on Kafka")
                    .check(classes);
        }
    }

    @Nested
    @DisplayName("Naming conventions")
    class NamingConventions {

        @Test
        @DisplayName("Controllers must be in web.controller package")
        void controllersMustBeInCorrectPackage() {
            classes()
                    .that().haveSimpleNameEndingWith("Controller")
                    .should().resideInAPackage("com.guitargpt.infrastructure.web.controller..")
                    .because("Controllers belong in the web.controller package")
                    .check(classes);
        }

        @Test
        @DisplayName("JPA entities must be in persistence.entity package")
        void jpaEntitiesMustBeInCorrectPackage() {
            classes()
                    .that().haveSimpleNameEndingWith("JpaEntity")
                    .should().resideInAPackage("com.guitargpt.infrastructure.persistence.entity..")
                    .because("JPA entities belong in the persistence.entity package")
                    .check(classes);
        }

        @Test
        @DisplayName("Repository adapters must be in persistence.adapter package")
        void repositoryAdaptersMustBeInCorrectPackage() {
            classes()
                    .that().haveSimpleNameEndingWith("RepositoryAdapter")
                    .should().resideInAPackage("com.guitargpt.infrastructure.persistence.adapter..")
                    .because("Repository adapters belong in the persistence.adapter package")
                    .check(classes);
        }

        @Test
        @DisplayName("Services must be in application.service package")
        void servicesMustBeInCorrectPackage() {
            classes()
                    .that().haveSimpleNameEndingWith("Service")
                    .and().areNotInterfaces()
                    .should().resideInAPackage("com.guitargpt.application.service..")
                    .because("Services belong in the application.service package")
                    .check(classes);
        }
    }

    @Nested
    @DisplayName("Dependency direction rules")
    class DependencyDirectionRules {

        @Test
        @DisplayName("Controllers must not access repositories directly")
        void controllersMustNotAccessRepositories() {
            noClasses()
                    .that().resideInAPackage("com.guitargpt.infrastructure.web..")
                    .should().dependOnClassesThat().resideInAPackage("com.guitargpt.infrastructure.persistence..")
                    .because("Controllers must use use cases, not repositories directly")
                    .check(classes);
        }

        @Test
        @DisplayName("Controllers must depend on use case ports, not services directly")
        void controllersMustDependOnPorts() {
            noClasses()
                    .that().resideInAPackage("com.guitargpt.infrastructure.web..")
                    .should().dependOnClassesThat().resideInAPackage("com.guitargpt.application..")
                    .because("Controllers must depend on domain ports (use cases), not application services")
                    .check(classes);
        }
    }
}
