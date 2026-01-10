package com.example.audit.unit.event;

import com.example.audit.domain.event.AuditableDomainEvent;
import com.example.audit.domain.event.BaseAuditEvent;
import com.example.audit.domain.model.AuditLog;
import com.example.audit.domain.model.AuditResult;
import com.example.audit.domain.port.AuditLogRepository;
import com.example.audit.infrastructure.config.AuditProperties;
import com.example.audit.infrastructure.event.AuditDomainEventListener;
import com.example.audit.infrastructure.metrics.AuditMetrics;
import com.example.audit.infrastructure.processor.PayloadProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditDomainEventListener Tests")
class AuditDomainEventListenerTest {

    @Mock
    private AuditLogRepository repository;

    @Mock
    private AuditMetrics metrics;

    private PayloadProcessor payloadProcessor;
    private AuditDomainEventListener listener;

    @BeforeEach
    void setUp() {
        AuditProperties properties = new AuditProperties();
        properties.getPayload().setMaxSize(65536);
        payloadProcessor = new PayloadProcessor(new ObjectMapper(), properties);
        listener = new AuditDomainEventListener(repository, payloadProcessor, metrics);
    }

    @Nested
    @DisplayName("handleAuditEvent()")
    class HandleAuditEventTests {

        @Test
        @DisplayName("should persist audit log from domain event")
        void shouldPersistAuditLogFromDomainEvent() {
            // given
            AuditableDomainEvent event = createSuccessEvent();

            // when
            listener.handleAuditEvent(event);

            // then
            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(repository).save(captor.capture());

            AuditLog savedLog = captor.getValue();
            assertThat(savedLog.id().value()).isEqualTo(event.eventId());
            assertThat(savedLog.timestamp()).isEqualTo(event.occurredAt());
            assertThat(savedLog.eventType().value()).isEqualTo("PRODUCT_CREATED");
            assertThat(savedLog.aggregateType()).isEqualTo("Product");
            assertThat(savedLog.aggregateId()).isEqualTo("prod-123");
            assertThat(savedLog.username()).isEqualTo("admin@example.com");
            assertThat(savedLog.serviceName()).isEqualTo("product-service");
            assertThat(savedLog.action()).isEqualTo("createProduct");
            assertThat(savedLog.result()).isEqualTo(AuditResult.SUCCESS);
        }

        @Test
        @DisplayName("should record metrics on success")
        void shouldRecordMetricsOnSuccess() {
            // given
            AuditableDomainEvent event = createSuccessEvent();

            // when
            listener.handleAuditEvent(event);

            // then
            verify(metrics).recordLatency(anyLong());
            verify(metrics).incrementTotal();
            verify(metrics, never()).incrementFailed();
        }

        @Test
        @DisplayName("should handle failure event correctly")
        void shouldHandleFailureEventCorrectly() {
            // given
            AuditableDomainEvent event = createFailureEvent();

            // when
            listener.handleAuditEvent(event);

            // then
            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(repository).save(captor.capture());

            AuditLog savedLog = captor.getValue();
            assertThat(savedLog.result()).isEqualTo(AuditResult.FAILURE);
            assertThat(savedLog.errorMessage()).isEqualTo("Validation failed");
        }

        @Test
        @DisplayName("should not throw exception when repository fails")
        void shouldNotThrowExceptionWhenRepositoryFails() {
            // given
            AuditableDomainEvent event = createSuccessEvent();
            doThrow(new RuntimeException("DB connection failed"))
                    .when(repository).save(any());

            // when/then - should not throw
            assertThatNoException().isThrownBy(() -> listener.handleAuditEvent(event));
            verify(metrics).incrementFailed();
        }

        @Test
        @DisplayName("should handle event with null payload")
        void shouldHandleEventWithNullPayload() {
            // given
            AuditableDomainEvent event = new TestAuditEvent(
                    "EVENT_TYPE",
                    "Aggregate",
                    "id-123",
                    "someAction",
                    "user@example.com",
                    "test-service",
                    null,
                    true,
                    null
            );

            // when
            listener.handleAuditEvent(event);

            // then
            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().payload()).isNull();
        }

        @Test
        @DisplayName("should handle event with empty mask fields")
        void shouldHandleEventWithEmptyMaskFields() {
            // given
            AuditableDomainEvent event = createSuccessEvent();

            // when
            listener.handleAuditEvent(event);

            // then
            verify(repository).save(any(AuditLog.class));
        }
    }

    @Nested
    @DisplayName("handleAuditEventSync()")
    class HandleAuditEventSyncTests {

        @Test
        @DisplayName("should return true on successful persistence")
        void shouldReturnTrueOnSuccessfulPersistence() {
            // given
            AuditableDomainEvent event = createSuccessEvent();

            // when
            boolean result = listener.handleAuditEventSync(event);

            // then
            assertThat(result).isTrue();
            verify(repository).save(any(AuditLog.class));
        }

        @Test
        @DisplayName("should return false when persistence fails")
        void shouldReturnFalseWhenPersistenceFails() {
            // given
            AuditableDomainEvent event = createSuccessEvent();
            doThrow(new RuntimeException("DB error")).when(repository).save(any());

            // when
            boolean result = listener.handleAuditEventSync(event);

            // then
            assertThat(result).isFalse();
            verify(metrics).incrementFailed();
        }
    }

    private AuditableDomainEvent createSuccessEvent() {
        return new TestAuditEvent(
                "PRODUCT_CREATED",
                "Product",
                "prod-123",
                "createProduct",
                "admin@example.com",
                "product-service",
                "{\"name\":\"Test Product\"}",
                true,
                null
        );
    }

    private AuditableDomainEvent createFailureEvent() {
        return new TestAuditEvent(
                "PRODUCT_CREATION_FAILED",
                "Product",
                null,
                "createProduct",
                "admin@example.com",
                "product-service",
                "{\"name\":\"Test\"}",
                false,
                "Validation failed"
        );
    }

    private static class TestAuditEvent extends BaseAuditEvent {
        TestAuditEvent(String eventType, String aggregateType, String aggregateId,
                       String action, String username, String serviceName,
                       String payload, boolean success, String errorMessage) {
            super(eventType, aggregateType, aggregateId, action, username, serviceName,
                    payload, success, errorMessage);
        }
    }
}
