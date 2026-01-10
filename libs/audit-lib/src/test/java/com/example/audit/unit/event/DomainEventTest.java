package com.example.audit.unit.event;

import com.example.audit.domain.event.AuditableDomainEvent;
import com.example.audit.domain.event.BaseAuditEvent;
import com.example.audit.domain.event.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Domain Event Tests")
class DomainEventTest {

    @Nested
    @DisplayName("BaseAuditEvent")
    class BaseAuditEventTest {

        @Test
        @DisplayName("should create event with all required fields")
        void shouldCreateEventWithAllRequiredFields() {
            // when
            AuditableDomainEvent event = new TestAuditEvent(
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

            // then
            assertThat(event.eventId()).isNotNull();
            assertThat(event.occurredAt()).isBeforeOrEqualTo(Instant.now());
            assertThat(event.eventType()).isEqualTo("PRODUCT_CREATED");
            assertThat(event.aggregateType()).isEqualTo("Product");
            assertThat(event.aggregateId()).isEqualTo("prod-123");
            assertThat(event.action()).isEqualTo("createProduct");
            assertThat(event.username()).isEqualTo("admin@example.com");
            assertThat(event.serviceName()).isEqualTo("product-service");
            assertThat(event.payload()).isEqualTo("{\"name\":\"Test Product\"}");
            assertThat(event.isSuccess()).isTrue();
            assertThat(event.errorMessage()).isNull();
        }

        @Test
        @DisplayName("should create failure event with error message")
        void shouldCreateFailureEventWithErrorMessage() {
            // when
            AuditableDomainEvent event = new TestAuditEvent(
                    "PRODUCT_CREATION_FAILED",
                    "Product",
                    null,
                    "createProduct",
                    "admin@example.com",
                    "product-service",
                    "{\"name\":\"Test\"}",
                    false,
                    "Validation failed: name too short"
            );

            // then
            assertThat(event.isSuccess()).isFalse();
            assertThat(event.errorMessage()).isEqualTo("Validation failed: name too short");
            assertThat(event.aggregateId()).isNull();
        }

        @Test
        @DisplayName("should generate unique event IDs")
        void shouldGenerateUniqueEventIds() {
            // when
            AuditableDomainEvent event1 = new TestAuditEvent(
                    "EVENT_TYPE", "Aggregate", "id1", "action",
                    "user", "service", null, true, null
            );
            AuditableDomainEvent event2 = new TestAuditEvent(
                    "EVENT_TYPE", "Aggregate", "id2", "action",
                    "user", "service", null, true, null
            );

            // then
            assertThat(event1.eventId()).isNotEqualTo(event2.eventId());
        }

        @Test
        @DisplayName("should include optional context fields")
        void shouldIncludeOptionalContextFields() {
            // when
            AuditableDomainEvent event = new TestAuditEventWithContext(
                    "ORDER_PLACED",
                    "Order",
                    "order-456",
                    "placeOrder",
                    "customer@example.com",
                    "order-service",
                    "{\"total\":99.99}",
                    true,
                    null,
                    "192.168.1.100",
                    "corr-abc-123",
                    false,
                    new String[]{"creditCard"}
            );

            // then
            assertThat(event.clientIp()).isEqualTo("192.168.1.100");
            assertThat(event.correlationId()).isEqualTo("corr-abc-123");
            assertThat(event.isPayloadTruncated()).isFalse();
            assertThat(event.maskFields()).containsExactly("creditCard");
        }

        @Test
        @DisplayName("should throw exception for null required fields")
        void shouldThrowExceptionForNullRequiredFields() {
            assertThatThrownBy(() -> new TestAuditEvent(
                    null, "Aggregate", "id", "action", "user", "service", null, true, null
            )).isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("eventType");

            assertThatThrownBy(() -> new TestAuditEvent(
                    "EVENT", null, "id", "action", "user", "service", null, true, null
            )).isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("aggregateType");

            assertThatThrownBy(() -> new TestAuditEvent(
                    "EVENT", "Aggregate", "id", null, "user", "service", null, true, null
            )).isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("action");

            assertThatThrownBy(() -> new TestAuditEvent(
                    "EVENT", "Aggregate", "id", "action", null, "service", null, true, null
            )).isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("username");

            assertThatThrownBy(() -> new TestAuditEvent(
                    "EVENT", "Aggregate", "id", "action", "user", null, null, true, null
            )).isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("serviceName");
        }

        @Test
        @DisplayName("should provide meaningful toString")
        void shouldProvideMeaningfulToString() {
            // given
            AuditableDomainEvent event = new TestAuditEvent(
                    "PRODUCT_CREATED",
                    "Product",
                    "prod-123",
                    "createProduct",
                    "admin@example.com",
                    "product-service",
                    null,
                    true,
                    null
            );

            // when
            String str = event.toString();

            // then
            assertThat(str)
                    .contains("PRODUCT_CREATED")
                    .contains("Product")
                    .contains("prod-123")
                    .contains("createProduct")
                    .contains("success=true");
        }

        @Test
        @DisplayName("should have default empty mask fields")
        void shouldHaveDefaultEmptyMaskFields() {
            // given
            AuditableDomainEvent event = new TestAuditEvent(
                    "EVENT_TYPE", "Aggregate", "id", "action",
                    "user", "service", null, true, null
            );

            // then
            assertThat(event.maskFields()).isEmpty();
            assertThat(event.isPayloadTruncated()).isFalse();
        }
    }

    /**
     * Test implementation of BaseAuditEvent
     */
    private static class TestAuditEvent extends BaseAuditEvent {
        TestAuditEvent(String eventType, String aggregateType, String aggregateId,
                       String action, String username, String serviceName,
                       String payload, boolean success, String errorMessage) {
            super(eventType, aggregateType, aggregateId, action, username, serviceName,
                    payload, success, errorMessage);
        }
    }

    /**
     * Test implementation of BaseAuditEvent with all context fields
     */
    private static class TestAuditEventWithContext extends BaseAuditEvent {
        TestAuditEventWithContext(String eventType, String aggregateType, String aggregateId,
                                   String action, String username, String serviceName,
                                   String payload, boolean success, String errorMessage,
                                   String clientIp, String correlationId,
                                   boolean payloadTruncated, String[] maskFields) {
            super(eventType, aggregateType, aggregateId, action, username, serviceName,
                    payload, success, errorMessage, clientIp, correlationId,
                    payloadTruncated, maskFields);
        }
    }
}
