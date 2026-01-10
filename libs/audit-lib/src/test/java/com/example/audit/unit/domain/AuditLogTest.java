package com.example.audit.unit.domain;

import com.example.audit.domain.model.AuditEventType;
import com.example.audit.domain.model.AuditLog;
import com.example.audit.domain.model.AuditLogId;
import com.example.audit.domain.model.AuditResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuditLog Domain Entity Tests")
class AuditLogTest {

    @Nested
    @DisplayName("Builder Validation")
    class BuilderValidationTests {

        @Test
        @DisplayName("should create AuditLog with all required fields")
        void shouldCreateAuditLogWithRequiredFields() {
            // Given
            AuditLogId id = AuditLogId.generate();
            Instant timestamp = Instant.now();
            AuditEventType eventType = AuditEventType.of("PRODUCT_CREATED");

            // When
            AuditLog auditLog = AuditLog.builder()
                    .id(id)
                    .timestamp(timestamp)
                    .eventType(eventType)
                    .aggregateType("Product")
                    .username("admin@example.com")
                    .serviceName("product-service")
                    .result(AuditResult.SUCCESS)
                    .build();

            // Then
            assertNotNull(auditLog);
            assertEquals(id, auditLog.id());
            assertEquals(timestamp, auditLog.timestamp());
            assertEquals(eventType, auditLog.eventType());
            assertEquals("Product", auditLog.aggregateType());
            assertEquals("admin@example.com", auditLog.username());
            assertEquals("product-service", auditLog.serviceName());
            assertEquals(AuditResult.SUCCESS, auditLog.result());
        }

        @Test
        @DisplayName("should create AuditLog with all fields including optional")
        void shouldCreateAuditLogWithAllFields() {
            // Given/When
            AuditLog auditLog = AuditLog.builder()
                    .id(AuditLogId.generate())
                    .timestamp(Instant.now())
                    .eventType("ORDER_PLACED")
                    .aggregateType("Order")
                    .aggregateId("order-123")
                    .username("user@example.com")
                    .serviceName("order-service")
                    .action("placeOrder")
                    .payload("{\"items\": []}")
                    .result(AuditResult.SUCCESS)
                    .clientIp("192.168.1.100")
                    .correlationId("corr-abc-123")
                    .payloadTruncated(false)
                    .build();

            // Then
            assertEquals("order-123", auditLog.aggregateId());
            assertEquals("placeOrder", auditLog.action());
            assertEquals("{\"items\": []}", auditLog.payload());
            assertEquals("192.168.1.100", auditLog.clientIp());
            assertEquals("corr-abc-123", auditLog.correlationId());
            assertFalse(auditLog.isPayloadTruncated());
        }

        @Test
        @DisplayName("should throw exception when id is null")
        void shouldThrowExceptionWhenIdIsNull() {
            // When/Then
            assertThrows(NullPointerException.class, () -> AuditLog.builder()
                    .id(null)
                    .timestamp(Instant.now())
                    .eventType("TEST")
                    .aggregateType("Test")
                    .username("user")
                    .serviceName("service")
                    .result(AuditResult.SUCCESS)
                    .build());
        }

        @Test
        @DisplayName("should throw exception when timestamp is null")
        void shouldThrowExceptionWhenTimestampIsNull() {
            assertThrows(NullPointerException.class, () -> AuditLog.builder()
                    .id(AuditLogId.generate())
                    .timestamp(null)
                    .eventType("TEST")
                    .aggregateType("Test")
                    .username("user")
                    .serviceName("service")
                    .result(AuditResult.SUCCESS)
                    .build());
        }

        @Test
        @DisplayName("should throw exception when eventType is null")
        void shouldThrowExceptionWhenEventTypeIsNull() {
            assertThrows(NullPointerException.class, () -> AuditLog.builder()
                    .id(AuditLogId.generate())
                    .timestamp(Instant.now())
                    .eventType((AuditEventType) null)
                    .aggregateType("Test")
                    .username("user")
                    .serviceName("service")
                    .result(AuditResult.SUCCESS)
                    .build());
        }

        @Test
        @DisplayName("should throw exception when aggregateType is blank")
        void shouldThrowExceptionWhenAggregateTypeIsBlank() {
            assertThrows(IllegalArgumentException.class, () -> AuditLog.builder()
                    .id(AuditLogId.generate())
                    .timestamp(Instant.now())
                    .eventType("TEST")
                    .aggregateType("")
                    .username("user")
                    .serviceName("service")
                    .result(AuditResult.SUCCESS)
                    .build());
        }

        @Test
        @DisplayName("should throw exception when username is blank")
        void shouldThrowExceptionWhenUsernameIsBlank() {
            assertThrows(IllegalArgumentException.class, () -> AuditLog.builder()
                    .id(AuditLogId.generate())
                    .timestamp(Instant.now())
                    .eventType("TEST")
                    .aggregateType("Test")
                    .username("  ")
                    .serviceName("service")
                    .result(AuditResult.SUCCESS)
                    .build());
        }

        @Test
        @DisplayName("should throw exception when serviceName is null")
        void shouldThrowExceptionWhenServiceNameIsNull() {
            assertThrows(IllegalArgumentException.class, () -> AuditLog.builder()
                    .id(AuditLogId.generate())
                    .timestamp(Instant.now())
                    .eventType("TEST")
                    .aggregateType("Test")
                    .username("user")
                    .serviceName(null)
                    .result(AuditResult.SUCCESS)
                    .build());
        }

        @Test
        @DisplayName("should throw exception when result is null")
        void shouldThrowExceptionWhenResultIsNull() {
            assertThrows(NullPointerException.class, () -> AuditLog.builder()
                    .id(AuditLogId.generate())
                    .timestamp(Instant.now())
                    .eventType("TEST")
                    .aggregateType("Test")
                    .username("user")
                    .serviceName("service")
                    .result(null)
                    .build());
        }
    }

    @Nested
    @DisplayName("Failed Operation Audit")
    class FailedOperationTests {

        @Test
        @DisplayName("should capture failure result with error message")
        void shouldCaptureFailureWithErrorMessage() {
            // Given/When
            AuditLog auditLog = AuditLog.builder()
                    .id(AuditLogId.generate())
                    .timestamp(Instant.now())
                    .eventType("PRODUCT_UPDATE")
                    .aggregateType("Product")
                    .aggregateId("prod-999")
                    .username("user@example.com")
                    .serviceName("product-service")
                    .action("updateProduct")
                    .result(AuditResult.FAILURE)
                    .errorMessage("ProductNotFoundException: Product with id prod-999 not found")
                    .build();

            // Then
            assertEquals(AuditResult.FAILURE, auditLog.result());
            assertNotNull(auditLog.errorMessage());
            assertTrue(auditLog.errorMessage().contains("ProductNotFoundException"));
        }
    }

    @Nested
    @DisplayName("Equality and HashCode")
    class EqualityTests {

        @Test
        @DisplayName("should consider AuditLogs equal if they have same id")
        void shouldBeEqualIfSameId() {
            // Given
            AuditLogId id = AuditLogId.generate();
            Instant now = Instant.now();

            AuditLog log1 = AuditLog.builder()
                    .id(id)
                    .timestamp(now)
                    .eventType("TEST")
                    .aggregateType("Test")
                    .username("user1")
                    .serviceName("service")
                    .result(AuditResult.SUCCESS)
                    .build();

            AuditLog log2 = AuditLog.builder()
                    .id(id)
                    .timestamp(now.plusSeconds(100))
                    .eventType("TEST")
                    .aggregateType("Test")
                    .username("user2")
                    .serviceName("service")
                    .result(AuditResult.FAILURE)
                    .build();

            // Then
            assertEquals(log1, log2);
            assertEquals(log1.hashCode(), log2.hashCode());
        }

        @Test
        @DisplayName("should not be equal if different ids")
        void shouldNotBeEqualIfDifferentIds() {
            // Given
            Instant now = Instant.now();

            AuditLog log1 = AuditLog.builder()
                    .id(AuditLogId.generate())
                    .timestamp(now)
                    .eventType("TEST")
                    .aggregateType("Test")
                    .username("user")
                    .serviceName("service")
                    .result(AuditResult.SUCCESS)
                    .build();

            AuditLog log2 = AuditLog.builder()
                    .id(AuditLogId.generate())
                    .timestamp(now)
                    .eventType("TEST")
                    .aggregateType("Test")
                    .username("user")
                    .serviceName("service")
                    .result(AuditResult.SUCCESS)
                    .build();

            // Then
            assertNotEquals(log1, log2);
        }
    }
}
