package com.example.audit.contract;

import com.example.audit.application.dto.AuditLogView;
import com.example.audit.application.dto.PagedResponse;
import com.example.audit.application.service.AuditQueryService;
import com.example.audit.domain.model.AuditResult;
import com.example.audit.infrastructure.web.AuditQueryController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Base class for Spring Cloud Contract generated tests.
 *
 * <p>This class sets up the MockMvc environment and provides mock data
 * for contract verification tests.</p>
 */
public abstract class BaseContractTest {

    private AuditQueryService queryService;

    @BeforeEach
    public void setup() {
        queryService = mock(AuditQueryService.class);
        setupMockResponses();

        // Configure ObjectMapper for proper JSON serialization
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);

        AuditQueryController controller = new AuditQueryController(queryService);
        StandaloneMockMvcBuilder mockMvcBuilder = MockMvcBuilders
                .standaloneSetup(controller)
                .setMessageConverters(converter);
        RestAssuredMockMvc.standaloneSetup(mockMvcBuilder);
    }

    private void setupMockResponses() {
        // Sample audit log for testing
        AuditLogView sampleLog = new AuditLogView(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                Instant.parse("2026-01-10T08:30:00Z"),
                "PRODUCT_CREATED",
                "Product",
                "prod-12345",
                "admin@example.com",
                "product-service",
                "createProduct",
                "{\"productCode\":\"TEST-001\",\"productName\":\"Test Widget\"}",
                AuditResult.SUCCESS,
                null,
                "192.168.1.100",
                "corr-abc-123",
                false
        );

        AuditLogView sampleLog2 = new AuditLogView(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440001"),
                Instant.parse("2026-01-10T08:31:00Z"),
                "PRODUCT_UPDATED",
                "Product",
                "prod-12345",
                "admin@example.com",
                "product-service",
                "updateProduct",
                "{\"price\":199.99}",
                AuditResult.SUCCESS,
                null,
                "192.168.1.100",
                "corr-abc-123",
                false
        );

        // Mock findById
        when(queryService.findById(UUID.fromString("550e8400-e29b-41d4-a716-446655440000")))
                .thenReturn(Optional.of(sampleLog));
        when(queryService.findById(UUID.fromString("00000000-0000-0000-0000-000000000000")))
                .thenReturn(Optional.empty());

        // Mock findByUsername
        when(queryService.findByUsername(eq("admin@example.com"), anyInt(), anyInt()))
                .thenReturn(new PagedResponse<>(
                        List.of(sampleLog, sampleLog2),
                        0, 20, 2, 1, true, true
                ));

        // Mock findByEventType
        when(queryService.findByEventType(eq("PRODUCT_CREATED"), anyInt(), anyInt()))
                .thenReturn(new PagedResponse<>(
                        List.of(sampleLog),
                        0, 20, 1, 1, true, true
                ));

        // Mock findByCorrelationId
        when(queryService.findByCorrelationId("corr-abc-123"))
                .thenReturn(List.of(sampleLog, sampleLog2));
        when(queryService.findByCorrelationId("nonexistent"))
                .thenReturn(List.of());

        // Mock findByAggregate
        when(queryService.findByAggregate(eq("Product"), eq("prod-12345"), anyInt(), anyInt()))
                .thenReturn(new PagedResponse<>(
                        List.of(sampleLog, sampleLog2),
                        0, 20, 2, 1, true, true
                ));

        // Mock findByServiceName
        when(queryService.findByServiceName(eq("product-service"), anyInt(), anyInt()))
                .thenReturn(new PagedResponse<>(
                        List.of(sampleLog, sampleLog2),
                        0, 20, 2, 1, true, true
                ));

        // Mock findByTimeRange (default query)
        when(queryService.findByTimeRange(any(Instant.class), any(Instant.class), anyInt(), anyInt()))
                .thenReturn(new PagedResponse<>(
                        List.of(sampleLog, sampleLog2),
                        0, 20, 2, 1, true, true
                ));
    }
}
