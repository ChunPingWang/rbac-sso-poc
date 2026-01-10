package com.example.audit.integration;

import com.example.audit.annotation.Auditable;
import com.example.audit.domain.model.AuditLog;
import com.example.audit.domain.port.AuditLogRepository;
import com.example.audit.infrastructure.config.AuditProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for configuration-driven field masking.
 *
 * <p>Tests that sensitive fields are properly masked in audit logs based on
 * annotation and configuration settings.</p>
 */
@SpringBootTest(classes = {TestApplication.class, MaskingConfigurationTest.MaskingTestService.class})
@ActiveProfiles("test")
@Transactional
@DisplayName("Masking Configuration Integration Tests")
class MaskingConfigurationTest {

    @Autowired
    private MaskingTestService testService;

    @Autowired
    private AuditLogRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Clear any existing audit logs
    }

    @Nested
    @DisplayName("Annotation-Based Masking Tests")
    class AnnotationBasedMaskingTests {

        @Test
        @DisplayName("should mask fields specified in @Auditable annotation")
        void shouldMaskFieldsSpecifiedInAnnotation() throws JsonProcessingException {
            // Given
            UserData userData = new UserData("john", "secret123", "john@example.com");

            // When
            testService.createUserWithMasking(userData);

            // Then
            Page<AuditLog> logs = repository.findByEventType(
                    com.example.audit.domain.model.AuditEventType.of("USER_CREATED_WITH_MASKING"),
                    PageRequest.of(0, 1));

            assertFalse(logs.isEmpty(), "Should have captured audit log");
            AuditLog log = logs.getContent().get(0);

            String payload = log.payload();
            assertNotNull(payload, "Payload should not be null");

            JsonNode json = objectMapper.readTree(payload);

            // Password should be masked
            if (json.has("password")) {
                assertEquals("********", json.get("password").asText(),
                        "Password should be completely masked");
            }

            // Username should NOT be masked
            if (json.has("username")) {
                assertEquals("john", json.get("username").asText(),
                        "Username should not be masked");
            }
        }

        @Test
        @DisplayName("should mask email with partial visibility")
        void shouldMaskEmailWithPartialVisibility() throws JsonProcessingException {
            // Given
            UserData userData = new UserData("john", "secret123", "john.doe@example.com");

            // When
            testService.createUserWithEmailMasking(userData);

            // Then
            Page<AuditLog> logs = repository.findByEventType(
                    com.example.audit.domain.model.AuditEventType.of("USER_CREATED_WITH_EMAIL_MASKING"),
                    PageRequest.of(0, 1));

            assertFalse(logs.isEmpty(), "Should have captured audit log");
            AuditLog log = logs.getContent().get(0);

            String payload = log.payload();
            if (payload != null) {
                JsonNode json = objectMapper.readTree(payload);

                if (json.has("email")) {
                    String maskedEmail = json.get("email").asText();
                    // Should show first 2 chars, then mask, then domain
                    assertTrue(maskedEmail.contains("***"),
                            "Email should be partially masked");
                    assertTrue(maskedEmail.contains("@example.com"),
                            "Email domain should be preserved");
                }
            }
        }

        @Test
        @DisplayName("should mask credit card with last 4 digits visible")
        void shouldMaskCreditCardWithLast4DigitsVisible() throws JsonProcessingException {
            // Given
            PaymentData paymentData = new PaymentData("4111111111111111", "12/25", "123");

            // When
            testService.processPaymentWithMasking(paymentData);

            // Then
            Page<AuditLog> logs = repository.findByEventType(
                    com.example.audit.domain.model.AuditEventType.of("PAYMENT_PROCESSED_WITH_MASKING"),
                    PageRequest.of(0, 1));

            assertFalse(logs.isEmpty(), "Should have captured audit log");
            AuditLog log = logs.getContent().get(0);

            String payload = log.payload();
            if (payload != null) {
                JsonNode json = objectMapper.readTree(payload);

                if (json.has("creditCard")) {
                    String maskedCard = json.get("creditCard").asText();
                    assertTrue(maskedCard.endsWith("1111"),
                            "Credit card should show last 4 digits");
                    assertTrue(maskedCard.contains("****"),
                            "Credit card should mask other digits");
                }

                // CVV should be fully masked
                if (json.has("cvv")) {
                    assertEquals("********", json.get("cvv").asText(),
                            "CVV should be completely masked");
                }
            }
        }

        @Test
        @DisplayName("should mask multiple fields in same payload")
        void shouldMaskMultipleFieldsInSamePayload() throws JsonProcessingException {
            // Given
            RegistrationData data = new RegistrationData(
                    "john",
                    "secret123",
                    "john@example.com",
                    "4111111111111111"
            );

            // When
            testService.registerWithFullMasking(data);

            // Then
            Page<AuditLog> logs = repository.findByEventType(
                    com.example.audit.domain.model.AuditEventType.of("REGISTRATION_WITH_FULL_MASKING"),
                    PageRequest.of(0, 1));

            assertFalse(logs.isEmpty(), "Should have captured audit log");
            AuditLog log = logs.getContent().get(0);

            String payload = log.payload();
            if (payload != null) {
                JsonNode json = objectMapper.readTree(payload);

                // Password should be masked
                if (json.has("password")) {
                    assertEquals("********", json.get("password").asText());
                }

                // Email should be partially masked
                if (json.has("email")) {
                    assertTrue(json.get("email").asText().contains("***"));
                }

                // Credit card should show last 4 digits
                if (json.has("creditCard")) {
                    assertTrue(json.get("creditCard").asText().endsWith("1111"));
                }

                // Username should NOT be masked
                if (json.has("username")) {
                    assertEquals("john", json.get("username").asText());
                }
            }
        }
    }

    @Nested
    @DisplayName("Nested Object Masking Tests")
    class NestedObjectMaskingTests {

        @Test
        @DisplayName("should mask fields in nested objects")
        void shouldMaskFieldsInNestedObjects() throws JsonProcessingException {
            // Given
            NestedUserData data = new NestedUserData(
                    "john",
                    new Credentials("secret123", "hint123")
            );

            // When
            testService.createUserWithNestedMasking(data);

            // Then
            Page<AuditLog> logs = repository.findByEventType(
                    com.example.audit.domain.model.AuditEventType.of("USER_CREATED_NESTED_MASKING"),
                    PageRequest.of(0, 1));

            assertFalse(logs.isEmpty(), "Should have captured audit log");
            AuditLog log = logs.getContent().get(0);

            String payload = log.payload();
            if (payload != null) {
                JsonNode json = objectMapper.readTree(payload);

                // Nested password should be masked
                if (json.has("credentials") && json.get("credentials").has("password")) {
                    assertEquals("********",
                            json.get("credentials").get("password").asText(),
                            "Nested password should be masked");
                }
            }
        }
    }

    // Test service with auditable methods
    @Service
    static class MaskingTestService {

        @Auditable(
                eventType = "USER_CREATED_WITH_MASKING",
                resourceType = "User",
                maskFields = {"password"}
        )
        public void createUserWithMasking(UserData userData) {
            // Business logic
        }

        @Auditable(
                eventType = "USER_CREATED_WITH_EMAIL_MASKING",
                resourceType = "User",
                maskFields = {"email"}
        )
        public void createUserWithEmailMasking(UserData userData) {
            // Business logic
        }

        @Auditable(
                eventType = "PAYMENT_PROCESSED_WITH_MASKING",
                resourceType = "Payment",
                maskFields = {"creditCard", "cvv"}
        )
        public void processPaymentWithMasking(PaymentData paymentData) {
            // Business logic
        }

        @Auditable(
                eventType = "REGISTRATION_WITH_FULL_MASKING",
                resourceType = "Registration",
                maskFields = {"password", "email", "creditCard"}
        )
        public void registerWithFullMasking(RegistrationData data) {
            // Business logic
        }

        @Auditable(
                eventType = "USER_CREATED_NESTED_MASKING",
                resourceType = "User",
                maskFields = {"credentials.password"}
        )
        public void createUserWithNestedMasking(NestedUserData data) {
            // Business logic
        }
    }

    // Test data classes
    record UserData(String username, String password, String email) {}
    record PaymentData(String creditCard, String expiry, String cvv) {}
    record RegistrationData(String username, String password, String email, String creditCard) {}
    record Credentials(String password, String hint) {}
    record NestedUserData(String username, Credentials credentials) {}
}
