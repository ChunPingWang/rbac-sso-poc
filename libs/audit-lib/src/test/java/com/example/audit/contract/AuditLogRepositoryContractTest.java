package com.example.audit.contract;

import com.example.audit.domain.model.*;
import com.example.audit.domain.port.AuditLogRepository;
import com.example.audit.integration.TestApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
@DisplayName("AuditLogRepository Contract Tests")
class AuditLogRepositoryContractTest {

    @Autowired
    private AuditLogRepository repository;

    private AuditLog createTestAuditLog(String eventType, String username) {
        return AuditLog.builder()
                .id(AuditLogId.generate())
                .timestamp(Instant.now())
                .eventType(eventType)
                .aggregateType("TestEntity")
                .aggregateId("test-123")
                .username(username)
                .serviceName("test-service")
                .action("testAction")
                .payload("{\"test\": true}")
                .result(AuditResult.SUCCESS)
                .clientIp("127.0.0.1")
                .build();
    }

    @Nested
    @DisplayName("Save Operations")
    class SaveOperations {

        @Test
        @DisplayName("should save and retrieve audit log by ID")
        void shouldSaveAndRetrieveById() {
            // Given
            AuditLog auditLog = createTestAuditLog("SAVE_TEST", "save-user");

            // When
            AuditLog saved = repository.save(auditLog);
            Optional<AuditLog> retrieved = repository.findById(auditLog.id());

            // Then
            assertTrue(retrieved.isPresent());
            assertEquals(auditLog.id(), retrieved.get().id());
            assertEquals("SAVE_TEST", retrieved.get().eventType().value());
            assertEquals("save-user", retrieved.get().username());
        }

        @Test
        @DisplayName("should preserve all fields when saving")
        void shouldPreserveAllFields() {
            // Given
            AuditLog auditLog = AuditLog.builder()
                    .id(AuditLogId.generate())
                    .timestamp(Instant.now())
                    .eventType("FULL_FIELD_TEST")
                    .aggregateType("Product")
                    .aggregateId("prod-999")
                    .username("admin@test.com")
                    .serviceName("product-service")
                    .action("createProduct")
                    .payload("{\"name\": \"Test Product\"}")
                    .result(AuditResult.FAILURE)
                    .errorMessage("Validation failed")
                    .clientIp("192.168.1.100")
                    .correlationId("corr-abc-123")
                    .payloadTruncated(true)
                    .build();

            // When
            repository.save(auditLog);
            Optional<AuditLog> retrieved = repository.findById(auditLog.id());

            // Then
            assertTrue(retrieved.isPresent());
            AuditLog result = retrieved.get();
            assertEquals("FULL_FIELD_TEST", result.eventType().value());
            assertEquals("Product", result.aggregateType());
            assertEquals("prod-999", result.aggregateId());
            assertEquals("admin@test.com", result.username());
            assertEquals("product-service", result.serviceName());
            assertEquals("createProduct", result.action());
            assertTrue(result.payload().contains("Test Product"));
            assertEquals(AuditResult.FAILURE, result.result());
            assertEquals("Validation failed", result.errorMessage());
            assertEquals("192.168.1.100", result.clientIp());
            assertEquals("corr-abc-123", result.correlationId());
            assertTrue(result.isPayloadTruncated());
        }
    }

    @Nested
    @DisplayName("Query Operations")
    class QueryOperations {

        @Test
        @DisplayName("should find by username with pagination")
        void shouldFindByUsername() {
            // Given
            String uniqueUsername = "query-user-" + System.currentTimeMillis();
            repository.save(createTestAuditLog("QUERY_TEST_1", uniqueUsername));
            repository.save(createTestAuditLog("QUERY_TEST_2", uniqueUsername));
            repository.save(createTestAuditLog("QUERY_TEST_3", "other-user"));

            // When
            Page<AuditLog> result = repository.findByUsername(
                    uniqueUsername, PageRequest.of(0, 10));

            // Then
            assertEquals(2, result.getTotalElements());
            assertTrue(result.getContent().stream()
                    .allMatch(log -> log.username().equals(uniqueUsername)));
        }

        @Test
        @DisplayName("should find by event type")
        void shouldFindByEventType() {
            // Given
            String uniqueEvent = "EVENT_TYPE_TEST_" + System.currentTimeMillis();
            repository.save(createTestAuditLog(uniqueEvent, "user1"));
            repository.save(createTestAuditLog(uniqueEvent, "user2"));

            // When
            Page<AuditLog> result = repository.findByEventType(
                    AuditEventType.of(uniqueEvent), PageRequest.of(0, 10));

            // Then
            assertEquals(2, result.getTotalElements());
        }

        @Test
        @DisplayName("should find by time range")
        void shouldFindByTimeRange() {
            // Given
            Instant now = Instant.now();
            Instant startTime = now.minus(1, ChronoUnit.HOURS);
            Instant endTime = now.plus(1, ChronoUnit.HOURS);

            repository.save(createTestAuditLog("TIME_RANGE_TEST", "user"));

            // When
            Page<AuditLog> result = repository.findByTimestampBetween(
                    startTime, endTime, PageRequest.of(0, 100));

            // Then
            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("should find by correlation ID")
        void shouldFindByCorrelationId() {
            // Given
            String correlationId = "corr-" + System.currentTimeMillis();
            AuditLog log1 = AuditLog.builder()
                    .id(AuditLogId.generate())
                    .timestamp(Instant.now())
                    .eventType("CORR_TEST_1")
                    .aggregateType("Entity")
                    .username("user")
                    .serviceName("service")
                    .result(AuditResult.SUCCESS)
                    .correlationId(correlationId)
                    .build();

            AuditLog log2 = AuditLog.builder()
                    .id(AuditLogId.generate())
                    .timestamp(Instant.now())
                    .eventType("CORR_TEST_2")
                    .aggregateType("Entity")
                    .username("user")
                    .serviceName("service")
                    .result(AuditResult.SUCCESS)
                    .correlationId(correlationId)
                    .build();

            repository.save(log1);
            repository.save(log2);

            // When
            List<AuditLog> result = repository.findByCorrelationId(correlationId);

            // Then
            assertEquals(2, result.size());
            assertTrue(result.stream()
                    .allMatch(log -> correlationId.equals(log.correlationId())));
        }
    }

    @Nested
    @DisplayName("Append-Only Contract")
    class AppendOnlyContract {

        @Test
        @DisplayName("repository should not expose update methods")
        void shouldNotExposeUpdateMethods() {
            // The AuditLogRepository interface should not have update/delete methods
            // This test verifies the contract at compile time by not compiling if such methods exist
            // We can also verify through reflection that no such methods exist

            var methods = AuditLogRepository.class.getMethods();
            for (var method : methods) {
                String name = method.getName().toLowerCase();
                assertFalse(name.contains("update"), "Repository should not have update methods");
                assertFalse(name.contains("delete") && !name.equals("existsbyid"),
                        "Repository should not have delete methods");
            }
        }
    }
}
