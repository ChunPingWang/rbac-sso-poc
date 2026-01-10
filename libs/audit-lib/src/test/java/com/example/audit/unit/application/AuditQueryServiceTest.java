package com.example.audit.unit.application;

import com.example.audit.application.dto.AuditLogView;
import com.example.audit.application.dto.PagedResponse;
import com.example.audit.application.service.AuditQueryService;
import com.example.audit.domain.model.*;
import com.example.audit.domain.port.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditQueryService Tests")
class AuditQueryServiceTest {

    @Mock
    private AuditLogRepository repository;

    private AuditQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new AuditQueryService(repository);
    }

    private AuditLog createTestAuditLog(String eventType, String username) {
        return AuditLog.builder()
                .id(AuditLogId.generate())
                .timestamp(Instant.now())
                .eventType(eventType)
                .aggregateType("TestEntity")
                .aggregateId("test-123")
                .username(username)
                .serviceName("test-service")
                .result(AuditResult.SUCCESS)
                .build();
    }

    @Nested
    @DisplayName("Find By ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("should return audit log when found")
        void shouldReturnAuditLogWhenFound() {
            // Given
            AuditLogId id = AuditLogId.generate();
            AuditLog auditLog = AuditLog.builder()
                    .id(id)
                    .timestamp(Instant.now())
                    .eventType("TEST")
                    .aggregateType("Entity")
                    .username("user")
                    .serviceName("service")
                    .result(AuditResult.SUCCESS)
                    .build();

            when(repository.findById(id)).thenReturn(Optional.of(auditLog));

            // When
            Optional<AuditLogView> result = queryService.findById(id.value());

            // Then
            assertTrue(result.isPresent());
            assertEquals(id.value(), result.get().id());
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            // Given
            UUID id = UUID.randomUUID();
            when(repository.findById(any())).thenReturn(Optional.empty());

            // When
            Optional<AuditLogView> result = queryService.findById(id);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Find By Username Tests")
    class FindByUsernameTests {

        @Test
        @DisplayName("should return paginated results by username")
        void shouldReturnPaginatedResultsByUsername() {
            // Given
            List<AuditLog> logs = List.of(
                    createTestAuditLog("EVENT1", "admin"),
                    createTestAuditLog("EVENT2", "admin")
            );
            Page<AuditLog> page = new PageImpl<>(logs);
            when(repository.findByUsername(eq("admin"), any(Pageable.class)))
                    .thenReturn(page);

            // When
            PagedResponse<AuditLogView> result = queryService.findByUsername("admin", 0, 10);

            // Then
            assertEquals(2, result.content().size());
            assertEquals(2, result.totalElements());
            assertTrue(result.content().stream()
                    .allMatch(v -> "admin".equals(v.username())));
        }
    }

    @Nested
    @DisplayName("Find By Event Type Tests")
    class FindByEventTypeTests {

        @Test
        @DisplayName("should return paginated results by event type")
        void shouldReturnPaginatedResultsByEventType() {
            // Given
            List<AuditLog> logs = List.of(createTestAuditLog("PRODUCT_CREATED", "user"));
            Page<AuditLog> page = new PageImpl<>(logs);
            when(repository.findByEventType(any(AuditEventType.class), any(Pageable.class)))
                    .thenReturn(page);

            // When
            PagedResponse<AuditLogView> result = queryService.findByEventType("PRODUCT_CREATED", 0, 10);

            // Then
            assertEquals(1, result.content().size());
            assertEquals("PRODUCT_CREATED", result.content().get(0).eventType());
        }
    }

    @Nested
    @DisplayName("Find By Correlation ID Tests")
    class FindByCorrelationIdTests {

        @Test
        @DisplayName("should return related audit logs by correlation ID")
        void shouldReturnRelatedAuditLogs() {
            // Given
            String correlationId = "corr-123";
            List<AuditLog> logs = List.of(
                    AuditLog.builder()
                            .id(AuditLogId.generate())
                            .timestamp(Instant.now())
                            .eventType("EVENT1")
                            .aggregateType("Entity")
                            .username("user")
                            .serviceName("service")
                            .result(AuditResult.SUCCESS)
                            .correlationId(correlationId)
                            .build(),
                    AuditLog.builder()
                            .id(AuditLogId.generate())
                            .timestamp(Instant.now())
                            .eventType("EVENT2")
                            .aggregateType("Entity")
                            .username("user")
                            .serviceName("service")
                            .result(AuditResult.SUCCESS)
                            .correlationId(correlationId)
                            .build()
            );
            when(repository.findByCorrelationId(correlationId)).thenReturn(logs);

            // When
            List<AuditLogView> result = queryService.findByCorrelationId(correlationId);

            // Then
            assertEquals(2, result.size());
            assertTrue(result.stream()
                    .allMatch(v -> correlationId.equals(v.correlationId())));
        }
    }

    @Nested
    @DisplayName("Find By Time Range Tests")
    class FindByTimeRangeTests {

        @Test
        @DisplayName("should return paginated results within time range")
        void shouldReturnPaginatedResultsWithinTimeRange() {
            // Given
            Instant startTime = Instant.now().minusSeconds(3600);
            Instant endTime = Instant.now();
            List<AuditLog> logs = List.of(createTestAuditLog("EVENT", "user"));
            Page<AuditLog> page = new PageImpl<>(logs);
            when(repository.findByTimestampBetween(eq(startTime), eq(endTime), any(Pageable.class)))
                    .thenReturn(page);

            // When
            PagedResponse<AuditLogView> result = queryService.findByTimeRange(startTime, endTime, 0, 10);

            // Then
            assertEquals(1, result.content().size());
        }
    }
}
