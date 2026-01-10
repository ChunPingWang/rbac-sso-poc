package com.example.audit.unit.processor;

import com.example.audit.infrastructure.processor.FieldMasker;
import com.example.audit.infrastructure.processor.maskers.CreditCardFieldMasker;
import com.example.audit.infrastructure.processor.maskers.EmailFieldMasker;
import com.example.audit.infrastructure.processor.maskers.PasswordFieldMasker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FieldMasker implementations.
 *
 * <p>Tests various masking strategies for sensitive data.</p>
 */
@DisplayName("FieldMasker Tests")
class FieldMaskerTest {

    @Nested
    @DisplayName("PasswordFieldMasker Tests")
    class PasswordFieldMaskerTests {

        private final FieldMasker masker = new PasswordFieldMasker();

        @Test
        @DisplayName("should mask password completely")
        void shouldMaskPasswordCompletely() {
            String result = masker.mask("mySecretPassword123!");
            assertEquals("********", result);
        }

        @Test
        @DisplayName("should return masked value for any password")
        void shouldReturnMaskedValueForAnyPassword() {
            assertEquals("********", masker.mask("short"));
            assertEquals("********", masker.mask("a"));
            assertEquals("********", masker.mask("very-long-password-that-should-still-be-masked"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("should handle null and empty values")
        void shouldHandleNullAndEmptyValues(String value) {
            String result = masker.mask(value);
            assertEquals("********", result);
        }

        @Test
        @DisplayName("should support password field names")
        void shouldSupportPasswordFieldNames() {
            assertTrue(masker.supports("password"));
            assertTrue(masker.supports("PASSWORD"));
            assertTrue(masker.supports("Password"));
            assertTrue(masker.supports("userPassword"));
            assertTrue(masker.supports("user_password"));
            assertTrue(masker.supports("oldPassword"));
            assertTrue(masker.supports("newPassword"));
            assertTrue(masker.supports("confirmPassword"));
        }

        @Test
        @DisplayName("should support secret and credential field names")
        void shouldSupportSecretAndCredentialFieldNames() {
            assertTrue(masker.supports("secret"));
            assertTrue(masker.supports("apiSecret"));
            assertTrue(masker.supports("clientSecret"));
            assertTrue(masker.supports("credential"));
            assertTrue(masker.supports("credentials"));
        }

        @Test
        @DisplayName("should not support unrelated field names")
        void shouldNotSupportUnrelatedFieldNames() {
            assertFalse(masker.supports("username"));
            assertFalse(masker.supports("email"));
            assertFalse(masker.supports("name"));
            assertFalse(masker.supports("id"));
        }
    }

    @Nested
    @DisplayName("CreditCardFieldMasker Tests")
    class CreditCardFieldMaskerTests {

        private final FieldMasker masker = new CreditCardFieldMasker();

        @Test
        @DisplayName("should mask credit card with last 4 digits visible")
        void shouldMaskCreditCardWithLast4DigitsVisible() {
            String result = masker.mask("4111111111111111");
            assertEquals("****-****-****-1111", result);
        }

        @Test
        @DisplayName("should handle credit card with dashes")
        void shouldHandleCreditCardWithDashes() {
            String result = masker.mask("4111-1111-1111-1111");
            assertEquals("****-****-****-1111", result);
        }

        @Test
        @DisplayName("should handle credit card with spaces")
        void shouldHandleCreditCardWithSpaces() {
            String result = masker.mask("4111 1111 1111 1111");
            assertEquals("****-****-****-1111", result);
        }

        @Test
        @DisplayName("should handle short card numbers")
        void shouldHandleShortCardNumbers() {
            // Less than 4 digits should still be completely masked
            String result = masker.mask("123");
            assertEquals("****-****-****-****", result);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("should handle null and empty values")
        void shouldHandleNullAndEmptyValues(String value) {
            String result = masker.mask(value);
            assertEquals("****-****-****-****", result);
        }

        @Test
        @DisplayName("should support credit card field names")
        void shouldSupportCreditCardFieldNames() {
            assertTrue(masker.supports("creditCard"));
            assertTrue(masker.supports("credit_card"));
            assertTrue(masker.supports("creditCardNumber"));
            assertTrue(masker.supports("cardNumber"));
            assertTrue(masker.supports("card_number"));
            assertTrue(masker.supports("ccNumber"));
            assertTrue(masker.supports("cc_number"));
        }

        @Test
        @DisplayName("should support pan field names")
        void shouldSupportPanFieldNames() {
            assertTrue(masker.supports("pan"));
            assertTrue(masker.supports("PAN"));
            assertTrue(masker.supports("primaryAccountNumber"));
        }

        @Test
        @DisplayName("should not support unrelated field names")
        void shouldNotSupportUnrelatedFieldNames() {
            assertFalse(masker.supports("password"));
            assertFalse(masker.supports("email"));
            assertFalse(masker.supports("name"));
            assertFalse(masker.supports("id"));
        }
    }

    @Nested
    @DisplayName("EmailFieldMasker Tests")
    class EmailFieldMaskerTests {

        private final FieldMasker masker = new EmailFieldMasker();

        @Test
        @DisplayName("should mask email with partial local part visible")
        void shouldMaskEmailWithPartialLocalPartVisible() {
            String result = masker.mask("john.doe@example.com");
            assertEquals("jo***@example.com", result);
        }

        @Test
        @DisplayName("should handle short local part")
        void shouldHandleShortLocalPart() {
            String result = masker.mask("a@example.com");
            assertEquals("a***@example.com", result);
        }

        @Test
        @DisplayName("should handle two character local part")
        void shouldHandleTwoCharacterLocalPart() {
            String result = masker.mask("ab@example.com");
            assertEquals("ab***@example.com", result);
        }

        @Test
        @DisplayName("should preserve domain completely")
        void shouldPreserveDomainCompletely() {
            String result = masker.mask("longusername@subdomain.example.co.uk");
            assertEquals("lo***@subdomain.example.co.uk", result);
        }

        @Test
        @DisplayName("should handle email without @ symbol")
        void shouldHandleEmailWithoutAtSymbol() {
            String result = masker.mask("notanemail");
            assertEquals("***@***.***", result);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("should handle null and empty values")
        void shouldHandleNullAndEmptyValues(String value) {
            String result = masker.mask(value);
            assertEquals("***@***.***", result);
        }

        @Test
        @DisplayName("should support email field names")
        void shouldSupportEmailFieldNames() {
            assertTrue(masker.supports("email"));
            assertTrue(masker.supports("EMAIL"));
            assertTrue(masker.supports("Email"));
            assertTrue(masker.supports("emailAddress"));
            assertTrue(masker.supports("email_address"));
            assertTrue(masker.supports("userEmail"));
            assertTrue(masker.supports("user_email"));
        }

        @Test
        @DisplayName("should not support unrelated field names")
        void shouldNotSupportUnrelatedFieldNames() {
            assertFalse(masker.supports("password"));
            assertFalse(masker.supports("creditCard"));
            assertFalse(masker.supports("name"));
            assertFalse(masker.supports("id"));
        }
    }

    @Nested
    @DisplayName("Field Name Pattern Tests")
    class FieldNamePatternTests {

        @Test
        @DisplayName("should handle camelCase field names")
        void shouldHandleCamelCaseFieldNames() {
            FieldMasker passwordMasker = new PasswordFieldMasker();
            assertTrue(passwordMasker.supports("userPassword"));
            assertTrue(passwordMasker.supports("newPassword"));
        }

        @Test
        @DisplayName("should handle snake_case field names")
        void shouldHandleSnakeCaseFieldNames() {
            FieldMasker passwordMasker = new PasswordFieldMasker();
            assertTrue(passwordMasker.supports("user_password"));
            assertTrue(passwordMasker.supports("new_password"));
        }

        @Test
        @DisplayName("should be case insensitive")
        void shouldBeCaseInsensitive() {
            FieldMasker passwordMasker = new PasswordFieldMasker();
            assertTrue(passwordMasker.supports("PASSWORD"));
            assertTrue(passwordMasker.supports("Password"));
            assertTrue(passwordMasker.supports("password"));
        }
    }
}
