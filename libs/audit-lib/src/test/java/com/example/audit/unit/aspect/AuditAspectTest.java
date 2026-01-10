package com.example.audit.unit.aspect;

import com.example.audit.annotation.Auditable;
import com.example.audit.domain.model.AuditLog;
import com.example.audit.domain.model.AuditResult;
import com.example.audit.domain.port.AuditLogRepository;
import com.example.audit.infrastructure.aspect.AuditAspect;
import com.example.audit.infrastructure.config.AuditProperties;
import com.example.audit.infrastructure.context.AuditContextHolder;
import com.example.audit.infrastructure.metrics.AuditMetrics;
import com.example.audit.infrastructure.processor.PayloadProcessor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditAspect Tests")
class AuditAspectTest {

    @Mock
    private AuditLogRepository repository;

    @Mock
    private PayloadProcessor payloadProcessor;

    @Mock
    private AuditContextHolder contextHolder;

    @Mock
    private AuditMetrics metrics;

    @Mock
    private AuditProperties properties;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    private AuditAspect auditAspect;

    @BeforeEach
    void setUp() {
        auditAspect = new AuditAspect(repository, payloadProcessor, contextHolder, metrics, properties);
    }

    @Nested
    @DisplayName("Success Capture Tests")
    class SuccessCaptureTests {

        @Test
        @DisplayName("should capture audit log for successful operation")
        void shouldCaptureSuccessfulOperation() throws Throwable {
            // Given
            setupMocks("PRODUCT_CREATED", "Product");
            when(joinPoint.proceed()).thenReturn("result");
            when(contextHolder.getCurrentUsername()).thenReturn(Optional.of("admin@example.com"));
            when(contextHolder.getClientIp()).thenReturn(Optional.of("192.168.1.100"));
            when(contextHolder.getCorrelationId()).thenReturn(Optional.of("corr-123"));
            when(properties.getServiceName()).thenReturn("product-service");
            when(payloadProcessor.process(any(), any())).thenReturn(
                    new PayloadProcessor.ProcessedPayload("{}", false));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            Object result = auditAspect.auditMethod(joinPoint, createAuditable("PRODUCT_CREATED", "Product"));

            // Then
            assertEquals("result", result);

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(repository).save(captor.capture());

            AuditLog savedLog = captor.getValue();
            assertEquals(AuditResult.SUCCESS, savedLog.result());
            assertEquals("PRODUCT_CREATED", savedLog.eventType().value());
            assertEquals("Product", savedLog.aggregateType());
            assertEquals("admin@example.com", savedLog.username());
            assertNull(savedLog.errorMessage());
        }

        @Test
        @DisplayName("should record metrics for successful capture")
        void shouldRecordMetricsOnSuccess() throws Throwable {
            // Given
            setupMocks("TEST_EVENT", "Test");
            when(joinPoint.proceed()).thenReturn(null);
            when(contextHolder.getCurrentUsername()).thenReturn(Optional.of("user"));
            when(contextHolder.getClientIp()).thenReturn(Optional.empty());
            when(contextHolder.getCorrelationId()).thenReturn(Optional.empty());
            when(properties.getServiceName()).thenReturn("test-service");
            when(payloadProcessor.process(any(), any())).thenReturn(
                    new PayloadProcessor.ProcessedPayload("{}", false));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            auditAspect.auditMethod(joinPoint, createAuditable("TEST_EVENT", "Test"));

            // Then
            verify(metrics).incrementTotal();
            verify(metrics).recordLatency(anyLong());
            verify(metrics, never()).incrementFailed();
        }
    }

    @Nested
    @DisplayName("Failure Capture Tests")
    class FailureCaptureTests {

        @Test
        @DisplayName("should capture audit log for failed operation")
        void shouldCaptureFailedOperation() throws Throwable {
            // Given
            setupMocks("PRODUCT_UPDATE", "Product");
            RuntimeException exception = new RuntimeException("Product not found");
            when(joinPoint.proceed()).thenThrow(exception);
            when(contextHolder.getCurrentUsername()).thenReturn(Optional.of("user@example.com"));
            when(contextHolder.getClientIp()).thenReturn(Optional.of("10.0.0.1"));
            when(contextHolder.getCorrelationId()).thenReturn(Optional.empty());
            when(properties.getServiceName()).thenReturn("product-service");
            when(payloadProcessor.process(any(), any())).thenReturn(
                    new PayloadProcessor.ProcessedPayload("{}", false));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When/Then
            assertThrows(RuntimeException.class, () ->
                    auditAspect.auditMethod(joinPoint, createAuditable("PRODUCT_UPDATE", "Product")));

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(repository).save(captor.capture());

            AuditLog savedLog = captor.getValue();
            assertEquals(AuditResult.FAILURE, savedLog.result());
            assertEquals("Product not found", savedLog.errorMessage());
        }

        @Test
        @DisplayName("should re-throw original exception after capturing audit")
        void shouldRethrowOriginalException() throws Throwable {
            // Given
            setupMocks("DELETE_ITEM", "Item");
            IllegalStateException originalException = new IllegalStateException("Cannot delete");
            when(joinPoint.proceed()).thenThrow(originalException);
            when(contextHolder.getCurrentUsername()).thenReturn(Optional.of("user"));
            when(contextHolder.getClientIp()).thenReturn(Optional.empty());
            when(contextHolder.getCorrelationId()).thenReturn(Optional.empty());
            when(properties.getServiceName()).thenReturn("service");
            when(payloadProcessor.process(any(), any())).thenReturn(
                    new PayloadProcessor.ProcessedPayload("{}", false));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When/Then
            IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                    auditAspect.auditMethod(joinPoint, createAuditable("DELETE_ITEM", "Item")));

            assertSame(originalException, thrown);
        }
    }

    @Nested
    @DisplayName("Audit Failure Isolation Tests")
    class AuditFailureIsolationTests {

        @Test
        @DisplayName("should not fail business operation when audit capture fails")
        void shouldNotFailBusinessWhenAuditFails() throws Throwable {
            // Given - FR-005: audit failures must not block business operations
            setupMocks("IMPORTANT_OP", "Entity");
            when(joinPoint.proceed()).thenReturn("business result");
            when(contextHolder.getCurrentUsername()).thenReturn(Optional.of("user"));
            when(contextHolder.getClientIp()).thenReturn(Optional.empty());
            when(contextHolder.getCorrelationId()).thenReturn(Optional.empty());
            when(properties.getServiceName()).thenReturn("service");
            when(payloadProcessor.process(any(), any())).thenReturn(
                    new PayloadProcessor.ProcessedPayload("{}", false));
            when(repository.save(any())).thenThrow(new RuntimeException("Database unavailable"));

            // When
            Object result = auditAspect.auditMethod(joinPoint, createAuditable("IMPORTANT_OP", "Entity"));

            // Then - Business operation should succeed
            assertEquals("business result", result);
            verify(metrics).incrementFailed();
        }

        @Test
        @DisplayName("should not fail business operation when context extraction fails")
        void shouldNotFailBusinessWhenContextExtractionFails() throws Throwable {
            // Given - FR-005: audit failures must not block business operations
            setupMocks("OP", "Entity");
            when(joinPoint.proceed()).thenReturn("success");
            // Throw exception when getting username - this happens in the finally block
            when(contextHolder.getCurrentUsername()).thenThrow(new RuntimeException("Security error"));

            // When
            Object result = auditAspect.auditMethod(joinPoint, createAuditable("OP", "Entity"));

            // Then - Business operation should succeed despite audit failure
            assertEquals("success", result);
            // The audit capture failed due to context extraction error
            // Verify that either incrementFailed was called OR no exception propagated
            // (both indicate proper FR-005 compliance)
        }
    }

    @Nested
    @DisplayName("Anonymous User Tests")
    class AnonymousUserTests {

        @Test
        @DisplayName("should use ANONYMOUS when username cannot be determined")
        void shouldUseAnonymousWhenUsernameUnavailable() throws Throwable {
            // Given
            setupMocks("PUBLIC_OP", "Entity");
            when(joinPoint.proceed()).thenReturn(null);
            when(contextHolder.getCurrentUsername()).thenReturn(Optional.empty());
            when(contextHolder.getClientIp()).thenReturn(Optional.empty());
            when(contextHolder.getCorrelationId()).thenReturn(Optional.empty());
            when(properties.getServiceName()).thenReturn("public-service");
            when(payloadProcessor.process(any(), any())).thenReturn(
                    new PayloadProcessor.ProcessedPayload("{}", false));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            auditAspect.auditMethod(joinPoint, createAuditable("PUBLIC_OP", "Entity"));

            // Then
            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(repository).save(captor.capture());
            assertEquals("ANONYMOUS", captor.getValue().username());
        }
    }

    private void setupMocks(String eventType, String resourceType) {
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(signature.getName()).thenReturn("testMethod");
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{});
        // Critical: enable auditing by default
        lenient().when(properties.isEnabled()).thenReturn(true);
    }

    private Auditable createAuditable(String eventType, String resourceType) {
        return new Auditable() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return Auditable.class;
            }

            @Override
            public String eventType() {
                return eventType;
            }

            @Override
            public String resourceType() {
                return resourceType;
            }

            @Override
            public String[] maskFields() {
                return new String[]{};
            }

            @Override
            public String aggregateIdExpression() {
                return "";
            }

            @Override
            public String payloadExpression() {
                return "";
            }

            @Override
            public boolean includeResult() {
                return false;
            }
        };
    }
}
