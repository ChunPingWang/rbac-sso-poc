package com.example.ecommerce.product.domain.model.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Money Value Object")
class MoneyTest {

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create from BigDecimal")
        void shouldCreateFromBigDecimal() {
            Money money = Money.of(BigDecimal.valueOf(100.00));
            assertEquals(new BigDecimal("100.00"), money.amount());
        }

        @Test
        @DisplayName("should create from double")
        void shouldCreateFromDouble() {
            Money money = Money.of(99.99);
            assertEquals(new BigDecimal("99.99"), money.amount());
        }

        @Test
        @DisplayName("should create from long")
        void shouldCreateFromLong() {
            Money money = Money.of(500L);
            assertEquals(new BigDecimal("500.00"), money.amount());
        }

        @Test
        @DisplayName("should create zero")
        void shouldCreateZero() {
            Money money = Money.zero();
            assertEquals(new BigDecimal("0.00"), money.amount());
            assertTrue(money.isZero());
        }

        @Test
        @DisplayName("should round to 2 decimal places")
        void shouldRoundToTwoDecimalPlaces() {
            Money money = Money.of(99.999);
            assertEquals(new BigDecimal("100.00"), money.amount());
        }

        @Test
        @DisplayName("should throw on null amount")
        void shouldThrowOnNullAmount() {
            assertThrows(NullPointerException.class, () -> Money.of((BigDecimal) null));
        }
    }

    @Nested
    @DisplayName("Arithmetic Operations")
    class ArithmeticOperations {

        @Test
        @DisplayName("should add two money values")
        void shouldAddTwoMoneyValues() {
            Money a = Money.of(100.00);
            Money b = Money.of(50.50);
            Money result = a.add(b);
            assertEquals(new BigDecimal("150.50"), result.amount());
        }

        @Test
        @DisplayName("should subtract two money values")
        void shouldSubtractTwoMoneyValues() {
            Money a = Money.of(100.00);
            Money b = Money.of(30.00);
            Money result = a.subtract(b);
            assertEquals(new BigDecimal("70.00"), result.amount());
        }

        @Test
        @DisplayName("should multiply by integer")
        void shouldMultiplyByInteger() {
            Money money = Money.of(25.00);
            Money result = money.multiply(3);
            assertEquals(new BigDecimal("75.00"), result.amount());
        }
    }

    @Nested
    @DisplayName("Comparison")
    class Comparison {

        @Test
        @DisplayName("should identify positive amount")
        void shouldIdentifyPositiveAmount() {
            assertTrue(Money.of(100.00).isPositive());
            assertFalse(Money.of(0).isPositive());
            assertFalse(Money.of(-10).isPositive());
        }

        @Test
        @DisplayName("should identify zero amount")
        void shouldIdentifyZeroAmount() {
            assertTrue(Money.of(0).isZero());
            assertFalse(Money.of(100).isZero());
        }

        @Test
        @DisplayName("should compare greater than")
        void shouldCompareGreaterThan() {
            Money a = Money.of(100.00);
            Money b = Money.of(50.00);
            assertTrue(a.isGreaterThan(b));
            assertFalse(b.isGreaterThan(a));
        }

        @Test
        @DisplayName("should compare less than")
        void shouldCompareLessThan() {
            Money a = Money.of(50.00);
            Money b = Money.of(100.00);
            assertTrue(a.isLessThan(b));
            assertFalse(b.isLessThan(a));
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("should pass validation for positive amount")
        void shouldPassValidationForPositiveAmount() {
            assertDoesNotThrow(() -> Money.of(100.00).validatePositive());
        }

        @Test
        @DisplayName("should fail validation for zero amount")
        void shouldFailValidationForZeroAmount() {
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Money.of(0).validatePositive()
            );
            assertTrue(ex.getMessage().contains("must be positive"));
        }

        @Test
        @DisplayName("should fail validation for negative amount")
        void shouldFailValidationForNegativeAmount() {
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Money.of(-10).validatePositive()
            );
            assertTrue(ex.getMessage().contains("must be positive"));
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal for same amount")
        void shouldBeEqualForSameAmount() {
            Money a = Money.of(100.00);
            Money b = Money.of(100.00);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("should not be equal for different amounts")
        void shouldNotBeEqualForDifferentAmounts() {
            Money a = Money.of(100.00);
            Money b = Money.of(50.00);
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("should format toString correctly")
        void shouldFormatToStringCorrectly() {
            Money money = Money.of(1234.56);
            assertEquals("NT$ 1234.56", money.toString());
        }
    }
}
