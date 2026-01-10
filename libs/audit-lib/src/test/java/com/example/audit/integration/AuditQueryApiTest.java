package com.example.audit.integration;

import com.example.audit.application.dto.AuditLogView;
import com.example.audit.application.dto.PagedResponse;
import com.example.audit.domain.model.*;
import com.example.audit.domain.port.AuditLogRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Audit Query REST API.
 *
 * <p>Tests the complete request/response cycle through the controller layer.</p>
 */
@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Audit Query API Integration Tests")
class AuditQueryApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    private AuditLog testLog1;
    private AuditLog testLog2;
    private AuditLog testLog3;

    @BeforeEach
    void setUp() {
        // Create test audit logs with different attributes for filtering
        testLog1 = repository.save(AuditLog.builder()
                .id(AuditLogId.generate())
                .timestamp(Instant.now().minus(1, ChronoUnit.HOURS))
                .eventType("USER_CREATED")
                .aggregateType("User")
                .aggregateId("user-123")
                .username("admin")
                .serviceName("user-service")
                .result(AuditResult.SUCCESS)
                .correlationId("corr-abc")
                .build());

        testLog2 = repository.save(AuditLog.builder()
                .id(AuditLogId.generate())
                .timestamp(Instant.now().minus(30, ChronoUnit.MINUTES))
                .eventType("USER_UPDATED")
                .aggregateType("User")
                .aggregateId("user-123")
                .username("admin")
                .serviceName("user-service")
                .result(AuditResult.SUCCESS)
                .correlationId("corr-abc")
                .build());

        testLog3 = repository.save(AuditLog.builder()
                .id(AuditLogId.generate())
                .timestamp(Instant.now())
                .eventType("PRODUCT_CREATED")
                .aggregateType("Product")
                .aggregateId("prod-456")
                .username("manager")
                .serviceName("product-service")
                .result(AuditResult.FAILURE)
                .errorMessage("Validation failed")
                .build());
    }

    @Nested
    @DisplayName("GET /api/v1/audit-logs/{id}")
    class GetByIdTests {

        @Test
        @DisplayName("should return audit log when found")
        void shouldReturnAuditLogWhenFound() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/audit-logs/{id}", testLog1.id().value())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            AuditLogView view = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    AuditLogView.class);

            assertEquals(testLog1.id().value(), view.id());
            assertEquals("USER_CREATED", view.eventType());
            assertEquals("admin", view.username());
        }

        @Test
        @DisplayName("should return 404 when not found")
        void shouldReturn404WhenNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/audit-logs/{id}", AuditLogId.generate().value())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/audit-logs")
    class QueryTests {

        @Test
        @DisplayName("should return paginated results with default filters")
        void shouldReturnPaginatedResultsWithDefaultFilters() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/audit-logs")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").isNumber())
                    .andReturn();

            PagedResponse<AuditLogView> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<PagedResponse<AuditLogView>>() {});

            assertTrue(response.totalElements() >= 3);
        }

        @Test
        @DisplayName("should filter by username")
        void shouldFilterByUsername() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/audit-logs")
                            .param("username", "admin")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            PagedResponse<AuditLogView> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<PagedResponse<AuditLogView>>() {});

            assertEquals(2, response.totalElements());
            assertTrue(response.content().stream()
                    .allMatch(v -> "admin".equals(v.username())));
        }

        @Test
        @DisplayName("should filter by event type")
        void shouldFilterByEventType() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/audit-logs")
                            .param("eventType", "PRODUCT_CREATED")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            PagedResponse<AuditLogView> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<PagedResponse<AuditLogView>>() {});

            assertEquals(1, response.totalElements());
            assertEquals("PRODUCT_CREATED", response.content().get(0).eventType());
        }

        @Test
        @DisplayName("should filter by aggregate type and ID")
        void shouldFilterByAggregate() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/audit-logs")
                            .param("aggregateType", "User")
                            .param("aggregateId", "user-123")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            PagedResponse<AuditLogView> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<PagedResponse<AuditLogView>>() {});

            assertEquals(2, response.totalElements());
            assertTrue(response.content().stream()
                    .allMatch(v -> "User".equals(v.aggregateType()) && "user-123".equals(v.aggregateId())));
        }

        @Test
        @DisplayName("should filter by service name")
        void shouldFilterByServiceName() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/audit-logs")
                            .param("serviceName", "product-service")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            PagedResponse<AuditLogView> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<PagedResponse<AuditLogView>>() {});

            assertEquals(1, response.totalElements());
            assertEquals("product-service", response.content().get(0).serviceName());
        }

        @Test
        @DisplayName("should filter by time range")
        void shouldFilterByTimeRange() throws Exception {
            Instant startTime = Instant.now().minus(2, ChronoUnit.HOURS);
            Instant endTime = Instant.now().minus(20, ChronoUnit.MINUTES);

            MvcResult result = mockMvc.perform(get("/api/v1/audit-logs")
                            .param("startTime", startTime.toString())
                            .param("endTime", endTime.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            PagedResponse<AuditLogView> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<PagedResponse<AuditLogView>>() {});

            // Should include testLog1 and testLog2 but not testLog3
            assertEquals(2, response.totalElements());
        }

        @Test
        @DisplayName("should respect pagination parameters")
        void shouldRespectPaginationParameters() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/audit-logs")
                            .param("page", "0")
                            .param("size", "2")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            PagedResponse<AuditLogView> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<PagedResponse<AuditLogView>>() {});

            assertEquals(2, response.content().size());
            assertEquals(0, response.page());
            assertEquals(2, response.size());
        }

        @Test
        @DisplayName("should clamp size to max 100")
        void shouldClampSizeToMax100() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/audit-logs")
                            .param("size", "200")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            PagedResponse<AuditLogView> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<PagedResponse<AuditLogView>>() {});

            // Size should be clamped, actual returned content may be less than 100
            assertTrue(response.size() <= 100);
        }

        @Test
        @DisplayName("should sort by timestamp descending")
        void shouldSortByTimestampDescending() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/audit-logs")
                            .param("username", "admin")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            PagedResponse<AuditLogView> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<PagedResponse<AuditLogView>>() {});

            List<AuditLogView> content = response.content();
            for (int i = 0; i < content.size() - 1; i++) {
                assertTrue(content.get(i).timestamp().compareTo(content.get(i + 1).timestamp()) >= 0,
                        "Results should be sorted by timestamp descending");
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/audit-logs/correlation/{correlationId}")
    class GetByCorrelationIdTests {

        @Test
        @DisplayName("should return related audit logs")
        void shouldReturnRelatedAuditLogs() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/audit-logs/correlation/{correlationId}", "corr-abc")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            List<AuditLogView> views = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<List<AuditLogView>>() {});

            assertEquals(2, views.size());
            assertTrue(views.stream().allMatch(v -> "corr-abc".equals(v.correlationId())));
        }

        @Test
        @DisplayName("should return empty list when no matches")
        void shouldReturnEmptyListWhenNoMatches() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/audit-logs/correlation/{correlationId}", "non-existent")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            List<AuditLogView> views = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<List<AuditLogView>>() {});

            assertTrue(views.isEmpty());
        }
    }

    @Nested
    @DisplayName("Response Format Tests")
    class ResponseFormatTests {

        @Test
        @DisplayName("should include all audit log fields in response")
        void shouldIncludeAllAuditLogFields() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/audit-logs/{id}", testLog3.id().value())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.eventType").value("PRODUCT_CREATED"))
                    .andExpect(jsonPath("$.aggregateType").value("Product"))
                    .andExpect(jsonPath("$.aggregateId").value("prod-456"))
                    .andExpect(jsonPath("$.username").value("manager"))
                    .andExpect(jsonPath("$.serviceName").value("product-service"))
                    .andExpect(jsonPath("$.result").value("FAILURE"))
                    .andExpect(jsonPath("$.errorMessage").value("Validation failed"))
                    .andReturn();
        }

        @Test
        @DisplayName("should include pagination metadata in response")
        void shouldIncludePaginationMetadata() throws Exception {
            mockMvc.perform(get("/api/v1/audit-logs")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.page").isNumber())
                    .andExpect(jsonPath("$.size").isNumber())
                    .andExpect(jsonPath("$.totalElements").isNumber())
                    .andExpect(jsonPath("$.totalPages").isNumber())
                    .andExpect(jsonPath("$.first").isBoolean())
                    .andExpect(jsonPath("$.last").isBoolean());
        }
    }
}
