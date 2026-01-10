package com.example.ecommerce.product.domain.model.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProductId Value Object")
class ProductIdTest {

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create from UUID")
        void shouldCreateFromUUID() {
            UUID uuid = UUID.randomUUID();
            ProductId id = ProductId.of(uuid);
            assertEquals(uuid, id.value());
        }

        @Test
        @DisplayName("should create from String")
        void shouldCreateFromString() {
            String uuidString = "550e8400-e29b-41d4-a716-446655440000";
            ProductId id = ProductId.of(uuidString);
            assertEquals(UUID.fromString(uuidString), id.value());
        }

        @Test
        @DisplayName("should generate unique IDs")
        void shouldGenerateUniqueIds() {
            ProductId id1 = ProductId.generate();
            ProductId id2 = ProductId.generate();

            assertNotNull(id1.value());
            assertNotNull(id2.value());
            assertNotEquals(id1, id2);
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("should reject null UUID")
        void shouldRejectNullUUID() {
            assertThrows(NullPointerException.class, () -> ProductId.of((UUID) null));
        }

        @Test
        @DisplayName("should reject invalid UUID string")
        void shouldRejectInvalidUUIDString() {
            assertThrows(IllegalArgumentException.class, () -> ProductId.of("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal for same UUID")
        void shouldBeEqualForSameUUID() {
            UUID uuid = UUID.randomUUID();
            ProductId a = ProductId.of(uuid);
            ProductId b = ProductId.of(uuid);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("should not be equal for different UUIDs")
        void shouldNotBeEqualForDifferentUUIDs() {
            ProductId a = ProductId.generate();
            ProductId b = ProductId.generate();
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("should return UUID string as toString")
        void shouldReturnUUIDStringAsToString() {
            UUID uuid = UUID.randomUUID();
            ProductId id = ProductId.of(uuid);
            assertEquals(uuid.toString(), id.toString());
        }
    }
}
