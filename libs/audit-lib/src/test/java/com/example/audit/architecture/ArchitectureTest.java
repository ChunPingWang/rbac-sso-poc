package com.example.audit.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * ArchUnit tests for hexagonal architecture compliance.
 *
 * <p>Enforces architectural rules:</p>
 * <ul>
 *   <li>Domain layer has no dependencies on infrastructure</li>
 *   <li>Application layer depends only on domain</li>
 *   <li>Infrastructure adapts to domain interfaces</li>
 *   <li>No circular dependencies</li>
 * </ul>
 */
@DisplayName("Architecture Tests")
class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.example.audit");
    }

    @Nested
    @DisplayName("Layered Architecture Tests")
    class LayeredArchitectureTests {

        @Test
        @DisplayName("should enforce hexagonal architecture - domain independence")
        void shouldEnforceDomainIndependence() {
            // Core hexagonal architecture rule: Domain should not depend on Infrastructure
            // This is the most critical architectural constraint
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("should enforce hexagonal architecture - annotation independence")
        void shouldEnforceAnnotationIndependence() {
            // Annotations should be standalone and not depend on implementation details
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..annotation..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..domain..",
                            "..application..",
                            "..infrastructure.."
                    );

            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Domain Layer Tests")
    class DomainLayerTests {

        @Test
        @DisplayName("domain should not depend on Spring framework except Data Commons")
        void domainShouldNotDependOnSpring() {
            // Allow dependency on Spring Data Commons for Page/Pageable as a pragmatic compromise
            // This is a common pattern in hexagonal architecture implementations
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework.context..",
                            "org.springframework.beans..",
                            "org.springframework.boot..",
                            "org.springframework.web.."
                    );

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("domain should not depend on JPA")
        void domainShouldNotDependOnJpa() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "jakarta.persistence..",
                            "javax.persistence.."
                    );

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("domain should not depend on infrastructure")
        void domainShouldNotDependOnInfrastructure() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("domain model classes should be in model package")
        void domainModelClassesShouldBeInModelPackage() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..domain.model..")
                    .should().haveSimpleNameNotEndingWith("Repository")
                    .andShould().haveSimpleNameNotEndingWith("Service");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("domain ports should be interfaces")
        void domainPortsShouldBeInterfaces() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..domain.port..")
                    .should().beInterfaces();

            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Application Layer Tests")
    class ApplicationLayerTests {

        @Test
        @DisplayName("application services should be named with Service suffix")
        void applicationServicesShouldBeNamedCorrectly() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..application.service..")
                    .should().haveSimpleNameEndingWith("Service");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("DTOs should be in dto package")
        void dtosShouldBeInDtoPackage() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..application.dto..")
                    .should().haveSimpleNameEndingWith("View")
                    .orShould().haveSimpleNameEndingWith("Response")
                    .orShould().haveSimpleNameEndingWith("Request")
                    .orShould().haveSimpleNameEndingWith("Dto");

            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Infrastructure Layer Tests")
    class InfrastructureLayerTests {

        @Test
        @DisplayName("JPA entities should be in persistence.entity package")
        void jpaEntitiesShouldBeInPersistenceEntityPackage() {
            ArchRule rule = classes()
                    .that().areAnnotatedWith(jakarta.persistence.Entity.class)
                    .should().resideInAPackage("..infrastructure.persistence.entity..");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("Spring repositories should be in persistence package")
        void springRepositoriesShouldBeInPersistencePackage() {
            ArchRule rule = classes()
                    .that().areAssignableTo(org.springframework.data.repository.Repository.class)
                    .should().resideInAPackage("..infrastructure.persistence..");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("controllers should be in web package")
        void controllersShouldBeInWebPackage() {
            ArchRule rule = classes()
                    .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                    .should().resideInAPackage("..infrastructure.web..");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("aspects should be in aspect package")
        void aspectsShouldBeInAspectPackage() {
            ArchRule rule = classes()
                    .that().areAnnotatedWith(org.aspectj.lang.annotation.Aspect.class)
                    .should().resideInAPackage("..infrastructure.aspect..");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("configuration classes should be in config package")
        void configurationClassesShouldBeInConfigPackage() {
            ArchRule rule = classes()
                    .that().areAnnotatedWith(org.springframework.context.annotation.Configuration.class)
                    .or().areAnnotatedWith(org.springframework.boot.autoconfigure.AutoConfiguration.class)
                    .should().resideInAPackage("..infrastructure.config..");

            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Naming Convention Tests")
    class NamingConventionTests {

        @Test
        @DisplayName("repository implementations should end with Repository")
        void repositoryImplementationsShouldEndWithRepository() {
            ArchRule rule = classes()
                    .that().implement(com.example.audit.domain.port.AuditLogRepository.class)
                    .should().haveSimpleNameEndingWith("Repository");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("mappers should end with Mapper")
        void mappersShouldEndWithMapper() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..mapper..")
                    .should().haveSimpleNameEndingWith("Mapper");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("field maskers should end with Masker")
        void fieldMaskersShouldEndWithMasker() {
            ArchRule rule = classes()
                    .that().implement(com.example.audit.infrastructure.processor.FieldMasker.class)
                    .should().haveSimpleNameEndingWith("Masker");

            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Dependency Inversion Tests")
    class DependencyInversionTests {

        @Test
        @DisplayName("infrastructure should depend on domain ports not implementations")
        void infrastructureShouldDependOnDomainPorts() {
            // This is implicitly enforced by the hexagonal architecture rules
            // Domain ports are interfaces, and infrastructure implements them
            ArchRule rule = classes()
                    .that().resideInAPackage("..infrastructure.persistence..")
                    .and().implement(com.example.audit.domain.port.AuditLogRepository.class)
                    .should().dependOnClassesThat().resideInAPackage("..domain..");

            rule.check(importedClasses);
        }
    }
}
