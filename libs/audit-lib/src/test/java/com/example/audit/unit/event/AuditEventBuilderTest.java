package com.example.audit.unit.event;

import com.example.audit.application.event.AuditEventBuilder;
import com.example.audit.domain.event.AuditableDomainEvent;
import com.example.audit.infrastructure.config.AuditProperties;
import com.example.audit.infrastructure.context.AuditContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuditEventBuilder Tests")
class AuditEventBuilderTest {

    @Mock
    private AuditContextHolder contextHolder;

    private AuditProperties properties;
    private ObjectMapper objectMapper;
    private AuditEventBuilder builder;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        properties = new AuditProperties();
        properties.setServiceName("test-service");
        properties.getPayload().setMaxSize(65536);

        builder = new AuditEventBuilder(contextHolder, properties, objectMapper);
    }

    @Nested
    @DisplayName("success()")
    class SuccessEventTests {

        @Test
        @DisplayName("should create success event with required fields")
        void shouldCreateSuccessEventWithRequiredFields() {
            // given
            when(contextHolder.getCurrentUsername()).thenReturn(Optional.of("admin@example.com"));
            when(contextHolder.getClientIp()).thenReturn(Optional.of("192.168.1.100"));
            when(contextHolder.getCorrelationId()).thenReturn(Optional.of("corr-123"));

            // when
            AuditableDomainEvent event = builder.success()
                    .eventType("PRODUCT_CREATED")
                    .aggregateType("Product")
                    .aggregateId("prod-123")
                    .action("createProduct")
                    .payload("{\"name\":\"Test\"}")
                    .build();

            // then
            assertThat(event.isSuccess()).isTrue();
            assertThat(event.errorMessage()).isNull();
            assertThat(event.eventType()).isEqualTo("PRODUCT_CREATED");
            assertThat(event.aggregateType()).isEqualTo("Product");
            assertThat(event.aggregateId()).isEqualTo("prod-123");
            assertThat(event.action()).isEqualTo("createProduct");
            assertThat(event.username()).isEqualTo("admin@example.com");
            assertThat(event.serviceName()).isEqualTo("test-service");
            assertThat(event.clientIp()).isEqualTo("192.168.1.100");
            assertThat(event.correlationId()).isEqualTo("corr-123");
            assertThat(event.payload()).isEqualTo("{\"name\":\"Test\"}");
        }

        @Test
        @DisplayName("should use default username when not in context")
        void shouldUseDefaultUsernameWhenNotInContext() {
            // given
            when(contextHolder.getCurrentUsername()).thenReturn(Optional.empty());
            when(contextHolder.getClientIp()).thenReturn(Optional.empty());
            when(contextHolder.getCorrelationId()).thenReturn(Optional.empty());

            // when
            AuditableDomainEvent event = builder.success()
                    .eventType("EVENT")
                    .aggregateType("Type")
                    .action("action")
                    .build();

            // then
            assertThat(event.username()).isEqualTo("ANONYMOUS");
            assertThat(event.clientIp()).isEqualTo("unknown");
            assertThat(event.correlationId()).isNull();
        }

        @Test
        @DisplayName("should allow username override")
        void shouldAllowUsernameOverride() {
            // given
            when(contextHolder.getCurrentUsername()).thenReturn(Optional.of("context-user"));
            when(contextHolder.getClientIp()).thenReturn(Optional.empty());
            when(contextHolder.getCorrelationId()).thenReturn(Optional.empty());

            // when
            AuditableDomainEvent event = builder.success()
                    .eventType("EVENT")
                    .aggregateType("Type")
                    .action("action")
                    .username("override-user")
                    .build();

            // then
            assertThat(event.username()).isEqualTo("override-user");
        }

        @Test
        @DisplayName("should serialize object payload to JSON")
        void shouldSerializeObjectPayloadToJson() {
            // given
            when(contextHolder.getCurrentUsername()).thenReturn(Optional.of("user"));
            when(contextHolder.getClientIp()).thenReturn(Optional.empty());
            when(contextHolder.getCorrelationId()).thenReturn(Optional.empty());

            Map<String, Object> payload = Map.of(
                    "productCode", "P001",
                    "productName", "Test Product",
                    "price", 99.99
            );

            // when
            AuditableDomainEvent event = builder.success()
                    .eventType("PRODUCT_CREATED")
                    .aggregateType("Product")
                    .action("createProduct")
                    .payload(payload)
                    .build();

            // then
            assertThat(event.payload())
                    .contains("\"productCode\":\"P001\"")
                    .contains("\"productName\":\"Test Product\"")
                    .contains("\"price\":99.99");
        }
    }

    @Nested
    @DisplayName("failure()")
    class FailureEventTests {

        @Test
        @DisplayName("should create failure event with error message")
        void shouldCreateFailureEventWithErrorMessage() {
            // given
            when(contextHolder.getCurrentUsername()).thenReturn(Optional.of("user"));
            when(contextHolder.getClientIp()).thenReturn(Optional.empty());
            when(contextHolder.getCorrelationId()).thenReturn(Optional.empty());

            // when
            AuditableDomainEvent event = builder.failure("Validation failed")
                    .eventType("PRODUCT_CREATION_FAILED")
                    .aggregateType("Product")
                    .action("createProduct")
                    .build();

            // then
            assertThat(event.isSuccess()).isFalse();
            assertThat(event.errorMessage()).isEqualTo("Validation failed");
        }

        @Test
        @DisplayName("should create failure event from exception")
        void shouldCreateFailureEventFromException() {
            // given
            when(contextHolder.getCurrentUsername()).thenReturn(Optional.of("user"));
            when(contextHolder.getClientIp()).thenReturn(Optional.empty());
            when(contextHolder.getCorrelationId()).thenReturn(Optional.empty());

            Exception exception = new IllegalArgumentException("Invalid product code");

            // when
            AuditableDomainEvent event = builder.failure(exception)
                    .eventType("PRODUCT_CREATION_FAILED")
                    .aggregateType("Product")
                    .action("createProduct")
                    .build();

            // then
            assertThat(event.isSuccess()).isFalse();
            assertThat(event.errorMessage()).isEqualTo("Invalid product code");
        }
    }

    @Nested
    @DisplayName("Context overrides")
    class ContextOverrideTests {

        @Test
        @DisplayName("should allow all context fields to be overridden")
        void shouldAllowAllContextFieldsToBeOverridden() {
            // given
            when(contextHolder.getCurrentUsername()).thenReturn(Optional.of("context-user"));
            when(contextHolder.getClientIp()).thenReturn(Optional.of("context-ip"));
            when(contextHolder.getCorrelationId()).thenReturn(Optional.of("context-corr"));

            // when
            AuditableDomainEvent event = builder.success()
                    .eventType("EVENT")
                    .aggregateType("Type")
                    .action("action")
                    .username("override-user")
                    .clientIp("override-ip")
                    .correlationId("override-corr")
                    .build();

            // then
            assertThat(event.username()).isEqualTo("override-user");
            assertThat(event.clientIp()).isEqualTo("override-ip");
            assertThat(event.correlationId()).isEqualTo("override-corr");
        }

        @Test
        @DisplayName("should support mask fields configuration")
        void shouldSupportMaskFieldsConfiguration() {
            // given
            when(contextHolder.getCurrentUsername()).thenReturn(Optional.of("user"));
            when(contextHolder.getClientIp()).thenReturn(Optional.empty());
            when(contextHolder.getCorrelationId()).thenReturn(Optional.empty());

            // when
            AuditableDomainEvent event = builder.success()
                    .eventType("EVENT")
                    .aggregateType("Type")
                    .action("action")
                    .maskFields("password", "creditCard")
                    .build();

            // then
            assertThat(event.maskFields()).containsExactly("password", "creditCard");
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("should throw exception when eventType is null")
        void shouldThrowExceptionWhenEventTypeIsNull() {
            // given
            when(contextHolder.getCurrentUsername()).thenReturn(Optional.of("user"));
            when(contextHolder.getClientIp()).thenReturn(Optional.empty());
            when(contextHolder.getCorrelationId()).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> builder.success()
                    .aggregateType("Type")
                    .action("action")
                    .build())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("eventType");
        }

        @Test
        @DisplayName("should throw exception when aggregateType is null")
        void shouldThrowExceptionWhenAggregateTypeIsNull() {
            // given
            when(contextHolder.getCurrentUsername()).thenReturn(Optional.of("user"));
            when(contextHolder.getClientIp()).thenReturn(Optional.empty());
            when(contextHolder.getCorrelationId()).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> builder.success()
                    .eventType("EVENT")
                    .action("action")
                    .build())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("aggregateType");
        }

        @Test
        @DisplayName("should throw exception when action is null")
        void shouldThrowExceptionWhenActionIsNull() {
            // given
            when(contextHolder.getCurrentUsername()).thenReturn(Optional.of("user"));
            when(contextHolder.getClientIp()).thenReturn(Optional.empty());
            when(contextHolder.getCorrelationId()).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> builder.success()
                    .eventType("EVENT")
                    .aggregateType("Type")
                    .build())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("action");
        }
    }
}
