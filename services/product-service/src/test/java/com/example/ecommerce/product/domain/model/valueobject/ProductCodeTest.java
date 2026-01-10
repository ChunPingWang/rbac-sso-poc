package com.example.ecommerce.product.domain.model.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProductCode Value Object")
class ProductCodeTest {

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create valid product code")
        void shouldCreateValidProductCode() {
            ProductCode code = ProductCode.of("P000001");
            assertEquals("P000001", code.value());
        }

        @Test
        @DisplayName("should convert lowercase to uppercase")
        void shouldConvertLowercaseToUppercase() {
            ProductCode code = ProductCode.of("p000001");
            assertEquals("P000001", code.value());
        }

        @Test
        @DisplayName("should generate unique code")
        void shouldGenerateUniqueCode() {
            ProductCode code1 = ProductCode.generate();
            ProductCode code2 = ProductCode.generate();

            assertNotNull(code1.value());
            assertNotNull(code2.value());
            assertNotEquals(code1, code2);
        }

        @Test
        @DisplayName("should generate code matching pattern")
        void shouldGenerateCodeMatchingPattern() {
            ProductCode code = ProductCode.generate();
            assertTrue(code.value().matches("^P\\d{6}$"));
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("should reject null value")
        void shouldRejectNullValue() {
            assertThrows(NullPointerException.class, () -> ProductCode.of(null));
        }

        @Test
        @DisplayName("should reject code without P prefix")
        void shouldRejectCodeWithoutPPrefix() {
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> ProductCode.of("X000001")
            );
            assertTrue(ex.getMessage().contains("must match pattern"));
        }

        @Test
        @DisplayName("should reject code with wrong length")
        void shouldRejectCodeWithWrongLength() {
            assertThrows(IllegalArgumentException.class, () -> ProductCode.of("P00001"));
            assertThrows(IllegalArgumentException.class, () -> ProductCode.of("P0000001"));
        }

        @Test
        @DisplayName("should reject code with non-digits after P")
        void shouldRejectCodeWithNonDigitsAfterP() {
            assertThrows(IllegalArgumentException.class, () -> ProductCode.of("PABCDEF"));
            assertThrows(IllegalArgumentException.class, () -> ProductCode.of("P12345X"));
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal for same code")
        void shouldBeEqualForSameCode() {
            ProductCode a = ProductCode.of("P000001");
            ProductCode b = ProductCode.of("P000001");
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("should not be equal for different codes")
        void shouldNotBeEqualForDifferentCodes() {
            ProductCode a = ProductCode.of("P000001");
            ProductCode b = ProductCode.of("P000002");
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("should return value as toString")
        void shouldReturnValueAsToString() {
            ProductCode code = ProductCode.of("P123456");
            assertEquals("P123456", code.toString());
        }
    }
}
