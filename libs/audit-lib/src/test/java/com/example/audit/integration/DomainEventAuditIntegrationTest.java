package com.example.audit.integration;

import com.example.audit.application.event.AuditEventBuilder;
import com.example.audit.domain.event.AuditableDomainEvent;
import com.example.audit.domain.event.BaseAuditEvent;
import com.example.audit.domain.model.AuditLog;
import com.example.audit.domain.model.AuditResult;
import com.example.audit.domain.port.AuditEventPublisher;
import com.example.audit.domain.port.AuditLogRepository;
import com.example.audit.infrastructure.config.AuditAutoConfiguration;
import com.example.audit.infrastructure.config.AuditProperties;
import com.example.audit.infrastructure.context.AuditContextHolder;
import com.example.audit.infrastructure.event.AuditDomainEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = DomainEventAuditIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
@DisplayName("Domain Event Audit Integration Tests")
class DomainEventAuditIntegrationTest {

    @Configuration
    @EnableAutoConfiguration
    @Import(AuditAutoConfiguration.class)
    static class TestConfig {
    }

    @Autowired
    private AuditEventPublisher eventPublisher;

    @Autowired
    private AuditEventBuilder auditEventBuilder;

    @Autowired
    private AuditLogRepository repository;

    @Autowired
    private AuditDomainEventListener eventListener;

    @Autowired
    private AuditContextHolder contextHolder;

    @Autowired
    private AuditProperties properties;

    @BeforeEach
    void setUp() {
        properties.setServiceName("test-service");
    }

    @Nested
    @DisplayName("Domain Event Publishing")
    class DomainEventPublishingTests {

        @Test
        @DisplayName("should persist audit log when domain event is published")
        void shouldPersistAuditLogWhenDomainEventIsPublished() {
            // given
            AuditableDomainEvent event = new ProductCreatedEvent(
                    "prod-001",
                    "Test Product",
                    "admin@example.com",
                    "product-service"
            );

            // when - use sync listener to avoid async timing issues in test
            eventListener.handleAuditEventSync(event);

            // then
            Optional<AuditLog> savedLog = repository.findById(
                    com.example.audit.domain.model.AuditLogId.of(event.eventId())
            );

            assertThat(savedLog).isPresent();
            assertThat(savedLog.get().eventType().value()).isEqualTo("PRODUCT_CREATED");
            assertThat(savedLog.get().aggregateType()).isEqualTo("Product");
            assertThat(savedLog.get().aggregateId()).isEqualTo("prod-001");
            assertThat(savedLog.get().username()).isEqualTo("admin@example.com");
            assertThat(savedLog.get().result()).isEqualTo(AuditResult.SUCCESS);
        }

        @Test
        @DisplayName("should persist failure event with error message")
        void shouldPersistFailureEventWithErrorMessage() {
            // given
            AuditableDomainEvent event = new ProductCreationFailedEvent(
                    "Invalid product code format",
                    "admin@example.com",
                    "product-service"
            );

            // when
            eventListener.handleAuditEventSync(event);

            // then
            Optional<AuditLog> savedLog = repository.findById(
                    com.example.audit.domain.model.AuditLogId.of(event.eventId())
            );

            assertThat(savedLog).isPresent();
            assertThat(savedLog.get().eventType().value()).isEqualTo("PRODUCT_CREATION_FAILED");
            assertThat(savedLog.get().result()).isEqualTo(AuditResult.FAILURE);
            assertThat(savedLog.get().errorMessage()).isEqualTo("Invalid product code format");
        }
    }

    @Nested
    @DisplayName("AuditEventBuilder Integration")
    class AuditEventBuilderIntegrationTests {

        @Test
        @DisplayName("should create and persist event using builder")
        void shouldCreateAndPersistEventUsingBuilder() {
            // given - simulate security context
            contextHolder.setCurrentUsername("builder-user@example.com");
            contextHolder.setClientIp("10.0.0.1");
            contextHolder.setCorrelationId("corr-builder-test");

            try {
                // when
                AuditableDomainEvent event = auditEventBuilder.success()
                        .eventType("ORDER_PLACED")
                        .aggregateType("Order")
                        .aggregateId("order-999")
                        .action("placeOrder")
                        .payload("{\"total\": 199.99, \"items\": 3}")
                        .build();

                eventListener.handleAuditEventSync(event);

                // then
                Optional<AuditLog> savedLog = repository.findById(
                        com.example.audit.domain.model.AuditLogId.of(event.eventId())
                );

                assertThat(savedLog).isPresent();
                assertThat(savedLog.get().username()).isEqualTo("builder-user@example.com");
                assertThat(savedLog.get().clientIp()).isEqualTo("10.0.0.1");
                assertThat(savedLog.get().correlationId()).isEqualTo("corr-builder-test");
                assertThat(savedLog.get().serviceName()).isEqualTo("test-service");

            } finally {
                contextHolder.clear();
            }
        }

        @Test
        @DisplayName("should create failure event using builder")
        void shouldCreateFailureEventUsingBuilder() {
            // given
            contextHolder.setCurrentUsername("user@example.com");

            try {
                Exception ex = new IllegalArgumentException("Order total cannot be negative");

                // when
                AuditableDomainEvent event = auditEventBuilder.failure(ex)
                        .eventType("ORDER_PLACEMENT_FAILED")
                        .aggregateType("Order")
                        .action("placeOrder")
                        .build();

                eventListener.handleAuditEventSync(event);

                // then
                Optional<AuditLog> savedLog = repository.findById(
                        com.example.audit.domain.model.AuditLogId.of(event.eventId())
                );

                assertThat(savedLog).isPresent();
                assertThat(savedLog.get().result()).isEqualTo(AuditResult.FAILURE);
                assertThat(savedLog.get().errorMessage()).isEqualTo("Order total cannot be negative");

            } finally {
                contextHolder.clear();
            }
        }
    }

    @Nested
    @DisplayName("Query by Correlation ID")
    class CorrelationIdQueryTests {

        @Test
        @DisplayName("should find events by correlation ID")
        void shouldFindEventsByCorrelationId() {
            // given
            String correlationId = "test-corr-" + System.currentTimeMillis();

            AuditableDomainEvent event1 = new TestEventWithCorrelation(
                    "EVENT_ONE", "Type1", "id1", "action1",
                    "user1@test.com", "service1", correlationId
            );
            AuditableDomainEvent event2 = new TestEventWithCorrelation(
                    "EVENT_TWO", "Type2", "id2", "action2",
                    "user2@test.com", "service2", correlationId
            );

            eventListener.handleAuditEventSync(event1);
            eventListener.handleAuditEventSync(event2);

            // when
            List<AuditLog> logs = repository.findByCorrelationId(correlationId);

            // then
            assertThat(logs).hasSize(2);
            assertThat(logs).extracting(log -> log.eventType().value())
                    .containsExactlyInAnyOrder("EVENT_ONE", "EVENT_TWO");
        }
    }

    // Test event implementations
    private static class ProductCreatedEvent extends BaseAuditEvent {
        ProductCreatedEvent(String productId, String productName, String username, String serviceName) {
            super(
                    "PRODUCT_CREATED",
                    "Product",
                    productId,
                    "createProduct",
                    username,
                    serviceName,
                    "{\"productId\":\"" + productId + "\",\"productName\":\"" + productName + "\"}",
                    true,
                    null
            );
        }
    }

    private static class ProductCreationFailedEvent extends BaseAuditEvent {
        ProductCreationFailedEvent(String errorMessage, String username, String serviceName) {
            super(
                    "PRODUCT_CREATION_FAILED",
                    "Product",
                    null,
                    "createProduct",
                    username,
                    serviceName,
                    null,
                    false,
                    errorMessage
            );
        }
    }

    private static class TestEventWithCorrelation extends BaseAuditEvent {
        TestEventWithCorrelation(String eventType, String aggregateType, String aggregateId,
                                  String action, String username, String serviceName,
                                  String correlationId) {
            super(eventType, aggregateType, aggregateId, action, username, serviceName,
                    null, true, null, null, correlationId, false, null);
        }
    }
}
