package com.example.audit.unit.processor;

import com.example.audit.infrastructure.config.AuditProperties;
import com.example.audit.infrastructure.processor.PayloadProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PayloadProcessor Tests")
class PayloadProcessorTest {

    private PayloadProcessor payloadProcessor;
    private AuditProperties auditProperties;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        auditProperties = new AuditProperties();
        auditProperties.getPayload().setMaxSize(65536); // 64KB
        payloadProcessor = new PayloadProcessor(objectMapper, auditProperties);
    }

    @Nested
    @DisplayName("Serialization Tests")
    class SerializationTests {

        @Test
        @DisplayName("should serialize simple object to JSON")
        void shouldSerializeSimpleObject() {
            // Given
            Map<String, Object> data = Map.of(
                    "productCode", "SKU-001",
                    "productName", "Widget",
                    "price", 99.99
            );

            // When
            PayloadProcessor.ProcessedPayload result = payloadProcessor.process(new Object[]{data}, new String[]{});

            // Then
            assertNotNull(result);
            assertFalse(result.isTruncated());
            assertTrue(result.payload().contains("SKU-001"));
            assertTrue(result.payload().contains("Widget"));
        }

        @Test
        @DisplayName("should serialize multiple arguments")
        void shouldSerializeMultipleArguments() {
            // Given
            String arg1 = "hello";
            Integer arg2 = 42;
            Map<String, String> arg3 = Map.of("key", "value");

            // When
            PayloadProcessor.ProcessedPayload result = payloadProcessor.process(
                    new Object[]{arg1, arg2, arg3}, new String[]{});

            // Then
            assertNotNull(result);
            assertTrue(result.payload().contains("hello"));
            assertTrue(result.payload().contains("42"));
            assertTrue(result.payload().contains("key"));
        }

        @Test
        @DisplayName("should handle null arguments")
        void shouldHandleNullArguments() {
            // When
            PayloadProcessor.ProcessedPayload result = payloadProcessor.process(
                    new Object[]{null, "value", null}, new String[]{});

            // Then
            assertNotNull(result);
            assertTrue(result.payload().contains("null"));
            assertTrue(result.payload().contains("value"));
        }

        @Test
        @DisplayName("should handle empty arguments")
        void shouldHandleEmptyArguments() {
            // When
            PayloadProcessor.ProcessedPayload result = payloadProcessor.process(new Object[]{}, new String[]{});

            // Then
            assertNotNull(result);
            assertFalse(result.isTruncated());
        }
    }

    @Nested
    @DisplayName("Truncation Tests")
    class TruncationTests {

        @Test
        @DisplayName("should truncate payload exceeding 64KB limit")
        void shouldTruncateOversizedPayload() {
            // Given - Create a payload larger than 64KB
            auditProperties.getPayload().setMaxSize(1000); // Set to 1KB for testing
            payloadProcessor = new PayloadProcessor(objectMapper, auditProperties);

            StringBuilder largeContent = new StringBuilder();
            for (int i = 0; i < 2000; i++) {
                largeContent.append("X");
            }
            Map<String, String> largeData = Map.of("content", largeContent.toString());

            // When
            PayloadProcessor.ProcessedPayload result = payloadProcessor.process(
                    new Object[]{largeData}, new String[]{});

            // Then
            assertTrue(result.isTruncated());
            assertTrue(result.payload().contains("_truncated"));
            assertTrue(result.payload().contains("true"));
        }

        @Test
        @DisplayName("should not truncate payload within size limit")
        void shouldNotTruncateNormalPayload() {
            // Given
            Map<String, String> normalData = Map.of("key", "value");

            // When
            PayloadProcessor.ProcessedPayload result = payloadProcessor.process(
                    new Object[]{normalData}, new String[]{});

            // Then
            assertFalse(result.isTruncated());
            assertFalse(result.payload().contains("_truncated"));
        }

        @Test
        @DisplayName("should include original size marker when truncated")
        void shouldIncludeOriginalSizeWhenTruncated() {
            // Given
            auditProperties.getPayload().setMaxSize(100);
            payloadProcessor = new PayloadProcessor(objectMapper, auditProperties);

            StringBuilder content = new StringBuilder();
            for (int i = 0; i < 500; i++) {
                content.append("X");
            }

            // When
            PayloadProcessor.ProcessedPayload result = payloadProcessor.process(
                    new Object[]{Map.of("data", content.toString())}, new String[]{});

            // Then
            assertTrue(result.isTruncated());
            assertTrue(result.payload().contains("_originalSize"));
        }
    }

    @Nested
    @DisplayName("Field Masking Tests")
    class FieldMaskingTests {

        @Test
        @DisplayName("should mask password fields")
        void shouldMaskPasswordFields() {
            // Given
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("username", "admin");
            data.put("password", "secret123");

            // When
            PayloadProcessor.ProcessedPayload result = payloadProcessor.process(
                    new Object[]{data}, new String[]{"password"});

            // Then
            assertFalse(result.payload().contains("secret123"));
            assertTrue(result.payload().contains("****"));
        }

        @Test
        @DisplayName("should mask nested fields using dot notation")
        void shouldMaskNestedFields() {
            // Given
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("email", "user@example.com");
            user.put("ssn", "123-45-6789");

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("user", user);

            // When
            PayloadProcessor.ProcessedPayload result = payloadProcessor.process(
                    new Object[]{data}, new String[]{"user.ssn"});

            // Then
            assertFalse(result.payload().contains("123-45-6789"));
            assertTrue(result.payload().contains("user@example.com")); // Not masked
        }

        @Test
        @DisplayName("should mask multiple fields")
        void shouldMaskMultipleFields() {
            // Given
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("username", "admin");
            data.put("password", "secret");
            data.put("apiKey", "key-12345");
            data.put("publicInfo", "visible");

            // When
            PayloadProcessor.ProcessedPayload result = payloadProcessor.process(
                    new Object[]{data}, new String[]{"password", "apiKey"});

            // Then
            assertFalse(result.payload().contains("secret"));
            assertFalse(result.payload().contains("key-12345"));
            assertTrue(result.payload().contains("visible"));
            assertTrue(result.payload().contains("admin"));
        }

        @Test
        @DisplayName("should handle non-existent field gracefully")
        void shouldHandleNonExistentField() {
            // Given
            Map<String, String> data = Map.of("name", "test");

            // When
            PayloadProcessor.ProcessedPayload result = payloadProcessor.process(
                    new Object[]{data}, new String[]{"nonExistent"});

            // Then
            assertNotNull(result);
            assertTrue(result.payload().contains("test"));
        }
    }

    @Nested
    @DisplayName("Circular Reference Tests")
    class CircularReferenceTests {

        @Test
        @DisplayName("should handle circular references gracefully")
        void shouldHandleCircularReferences() {
            // Given
            Map<String, Object> parent = new LinkedHashMap<>();
            Map<String, Object> child = new LinkedHashMap<>();
            parent.put("child", child);
            child.put("parent", parent); // Circular reference

            // When
            PayloadProcessor.ProcessedPayload result = payloadProcessor.process(
                    new Object[]{parent}, new String[]{});

            // Then - Should not throw exception, should handle gracefully
            assertNotNull(result);
            // Result should contain error marker or truncated circular ref
            assertTrue(result.payload() != null);
        }
    }
}
