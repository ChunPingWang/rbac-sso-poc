package com.example.audit.integration;

import com.example.audit.domain.port.AuditLogRepository;
import com.example.audit.infrastructure.aspect.AuditAspect;
import com.example.audit.infrastructure.config.AuditProperties;
import com.example.audit.infrastructure.context.AuditContextHolder;
import com.example.audit.infrastructure.health.AuditHealthIndicator;
import com.example.audit.infrastructure.metrics.AuditMetrics;
import com.example.audit.infrastructure.processor.PayloadProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
@DisplayName("Auto-Configuration Tests")
class AutoConfigurationTest {

    @Autowired
    private ApplicationContext context;

    @Nested
    @DisplayName("Bean Registration Tests")
    class BeanRegistrationTests {

        @Test
        @DisplayName("should register AuditAspect bean")
        void shouldRegisterAuditAspect() {
            assertTrue(context.containsBean("auditAspect"));
            assertNotNull(context.getBean(AuditAspect.class));
        }

        @Test
        @DisplayName("should register AuditLogRepository bean")
        void shouldRegisterAuditLogRepository() {
            assertNotNull(context.getBean(AuditLogRepository.class));
        }

        @Test
        @DisplayName("should register PayloadProcessor bean")
        void shouldRegisterPayloadProcessor() {
            assertNotNull(context.getBean(PayloadProcessor.class));
        }

        @Test
        @DisplayName("should register AuditContextHolder bean")
        void shouldRegisterAuditContextHolder() {
            assertNotNull(context.getBean(AuditContextHolder.class));
        }

        @Test
        @DisplayName("should register AuditMetrics bean")
        void shouldRegisterAuditMetrics() {
            assertNotNull(context.getBean(AuditMetrics.class));
        }

        @Test
        @DisplayName("should register AuditHealthIndicator bean")
        void shouldRegisterAuditHealthIndicator() {
            assertNotNull(context.getBean(AuditHealthIndicator.class));
        }

        @Test
        @DisplayName("should register AuditProperties bean")
        void shouldRegisterAuditProperties() {
            AuditProperties props = context.getBean(AuditProperties.class);
            assertNotNull(props);
            assertTrue(props.isEnabled());
            // Service name comes from application-test.yml
            assertEquals("test-service", props.getServiceName());
        }
    }

    @Nested
    @DisplayName("Configuration Properties Tests")
    class ConfigurationPropertiesTests {

        @Autowired
        private AuditProperties auditProperties;

        @Test
        @DisplayName("should have default payload max size of 64KB")
        void shouldHaveDefaultPayloadMaxSize() {
            assertEquals(65536, auditProperties.getPayload().getMaxSize());
        }

        @Test
        @DisplayName("should have default masking fields")
        void shouldHaveDefaultMaskingFields() {
            assertFalse(auditProperties.getMasking().getDefaultFields().isEmpty());
            assertTrue(auditProperties.getMasking().getDefaultFields().contains("password"));
        }
    }
}
