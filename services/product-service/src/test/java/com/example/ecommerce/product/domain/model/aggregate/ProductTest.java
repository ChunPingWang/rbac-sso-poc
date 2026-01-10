package com.example.ecommerce.product.domain.model.aggregate;

import com.example.ecommerce.product.domain.event.*;
import com.example.ecommerce.product.domain.model.valueobject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Product Aggregate")
class ProductTest {

    private static final String TENANT_ID = "tenant-1";
    private static final String CREATED_BY = "admin";

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create product with valid data")
        void shouldCreateProductWithValidData() {
            Product product = Product.create(
                ProductCode.of("P000001"),
                "Test Product",
                Money.of(100.00),
                "Electronics",
                "A test product",
                TENANT_ID,
                CREATED_BY
            );

            assertNotNull(product.getId());
            assertEquals("P000001", product.getProductCode().value());
            assertEquals("Test Product", product.getName());
            assertEquals(Money.of(100.00), product.getPrice());
            assertEquals("Electronics", product.getCategory());
            assertEquals("A test product", product.getDescription());
            assertEquals(TENANT_ID, product.getTenantId());
            assertEquals(ProductStatus.ACTIVE, product.getStatus());
            assertTrue(product.isActive());
        }

        @Test
        @DisplayName("should register ProductCreated event")
        void shouldRegisterProductCreatedEvent() {
            Product product = Product.create(
                ProductCode.of("P000001"),
                "Test Product",
                Money.of(100.00),
                "Electronics",
                "Description",
                TENANT_ID,
                CREATED_BY
            );

            List<DomainEvent> events = product.pullDomainEvents();

            assertEquals(1, events.size());
            assertInstanceOf(ProductCreated.class, events.get(0));
            ProductCreated event = (ProductCreated) events.get(0);
            assertEquals("Test Product", event.name());
            assertEquals(Money.of(100.00), event.price());
        }

        @Test
        @DisplayName("should reject negative price on creation")
        void shouldRejectNegativePriceOnCreation() {
            assertThrows(IllegalArgumentException.class, () ->
                Product.create(
                    ProductCode.of("P000001"),
                    "Test Product",
                    Money.of(-100.00),
                    "Electronics",
                    "Description",
                    TENANT_ID,
                    CREATED_BY
                )
            );
        }

        @Test
        @DisplayName("should reject zero price on creation")
        void shouldRejectZeroPriceOnCreation() {
            assertThrows(IllegalArgumentException.class, () ->
                Product.create(
                    ProductCode.of("P000001"),
                    "Test Product",
                    Money.zero(),
                    "Electronics",
                    "Description",
                    TENANT_ID,
                    CREATED_BY
                )
            );
        }
    }

    @Nested
    @DisplayName("Update")
    class Update {

        private Product product;

        @BeforeEach
        void setUp() {
            product = Product.create(
                ProductCode.of("P000001"),
                "Original Name",
                Money.of(100.00),
                "Electronics",
                "Original Description",
                TENANT_ID,
                CREATED_BY
            );
            product.pullDomainEvents(); // Clear creation event
        }

        @Test
        @DisplayName("should update product fields")
        void shouldUpdateProductFields() {
            product.update("New Name", Money.of(200.00), "Clothing", "New Description", "updater");

            assertEquals("New Name", product.getName());
            assertEquals(Money.of(200.00), product.getPrice());
            assertEquals("Clothing", product.getCategory());
            assertEquals("New Description", product.getDescription());
            assertEquals("updater", product.getUpdatedBy());
        }

        @Test
        @DisplayName("should register ProductUpdated event")
        void shouldRegisterProductUpdatedEvent() {
            product.update("New Name", Money.of(200.00), "Clothing", "New Description", "updater");

            List<DomainEvent> events = product.pullDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(ProductUpdated.class, events.get(0));
        }

        @Test
        @DisplayName("should allow partial update")
        void shouldAllowPartialUpdate() {
            product.update("New Name", null, null, null, "updater");

            assertEquals("New Name", product.getName());
            assertEquals(Money.of(100.00), product.getPrice()); // Unchanged
            assertEquals("Electronics", product.getCategory()); // Unchanged
        }

        @Test
        @DisplayName("should reject negative price on update")
        void shouldRejectNegativePriceOnUpdate() {
            assertThrows(IllegalArgumentException.class, () ->
                product.update("Name", Money.of(-50.00), null, null, "updater")
            );
        }

        @Test
        @DisplayName("should not update deleted product")
        void shouldNotUpdateDeletedProduct() {
            product.delete(CREATED_BY);

            assertThrows(IllegalStateException.class, () ->
                product.update("New Name", null, null, null, "updater")
            );
        }
    }

    @Nested
    @DisplayName("Price Change")
    class PriceChange {

        private Product product;

        @BeforeEach
        void setUp() {
            product = Product.create(
                ProductCode.of("P000001"),
                "Test Product",
                Money.of(100.00),
                "Electronics",
                "Description",
                TENANT_ID,
                CREATED_BY
            );
            product.pullDomainEvents();
        }

        @Test
        @DisplayName("should change price")
        void shouldChangePrice() {
            product.changePrice(Money.of(150.00), "price-admin");

            assertEquals(Money.of(150.00), product.getPrice());
            assertEquals("price-admin", product.getUpdatedBy());
        }

        @Test
        @DisplayName("should register ProductPriceChanged event")
        void shouldRegisterProductPriceChangedEvent() {
            product.changePrice(Money.of(150.00), "price-admin");

            List<DomainEvent> events = product.pullDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(ProductPriceChanged.class, events.get(0));

            ProductPriceChanged event = (ProductPriceChanged) events.get(0);
            assertEquals(Money.of(100.00), event.oldPrice());
            assertEquals(Money.of(150.00), event.newPrice());
        }

        @Test
        @DisplayName("should reject negative price change")
        void shouldRejectNegativePriceChange() {
            assertThrows(IllegalArgumentException.class, () ->
                product.changePrice(Money.of(-50.00), "price-admin")
            );
        }

        @Test
        @DisplayName("should not change price on deleted product")
        void shouldNotChangePriceOnDeletedProduct() {
            product.delete(CREATED_BY);

            assertThrows(IllegalStateException.class, () ->
                product.changePrice(Money.of(150.00), "price-admin")
            );
        }
    }

    @Nested
    @DisplayName("Delete")
    class Delete {

        private Product product;

        @BeforeEach
        void setUp() {
            product = Product.create(
                ProductCode.of("P000001"),
                "Test Product",
                Money.of(100.00),
                "Electronics",
                "Description",
                TENANT_ID,
                CREATED_BY
            );
            product.pullDomainEvents();
        }

        @Test
        @DisplayName("should soft delete product")
        void shouldSoftDeleteProduct() {
            product.delete("admin");

            assertEquals(ProductStatus.DELETED, product.getStatus());
            assertFalse(product.isActive());
            assertEquals("admin", product.getUpdatedBy());
        }

        @Test
        @DisplayName("should register ProductDeleted event")
        void shouldRegisterProductDeletedEvent() {
            product.delete("admin");

            List<DomainEvent> events = product.pullDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(ProductDeleted.class, events.get(0));
        }

        @Test
        @DisplayName("should not delete already deleted product")
        void shouldNotDeleteAlreadyDeletedProduct() {
            product.delete("admin");

            assertThrows(IllegalStateException.class, () ->
                product.delete("admin")
            );
        }
    }

    @Nested
    @DisplayName("Status Management")
    class StatusManagement {

        private Product product;

        @BeforeEach
        void setUp() {
            product = Product.create(
                ProductCode.of("P000001"),
                "Test Product",
                Money.of(100.00),
                "Electronics",
                "Description",
                TENANT_ID,
                CREATED_BY
            );
            product.pullDomainEvents();
        }

        @Test
        @DisplayName("should deactivate product")
        void shouldDeactivateProduct() {
            product.deactivate();

            assertEquals(ProductStatus.INACTIVE, product.getStatus());
            assertFalse(product.isActive());
        }

        @Test
        @DisplayName("should activate inactive product")
        void shouldActivateInactiveProduct() {
            product.deactivate();
            product.activate();

            assertEquals(ProductStatus.ACTIVE, product.getStatus());
            assertTrue(product.isActive());
        }

        @Test
        @DisplayName("should not activate deleted product")
        void shouldNotActivateDeletedProduct() {
            product.delete(CREATED_BY);

            assertThrows(IllegalStateException.class, () ->
                product.activate()
            );
        }

        @Test
        @DisplayName("should not deactivate deleted product")
        void shouldNotDeactivateDeletedProduct() {
            product.delete(CREATED_BY);

            assertThrows(IllegalStateException.class, () ->
                product.deactivate()
            );
        }
    }

    @Nested
    @DisplayName("Domain Events")
    class DomainEvents {

        @Test
        @DisplayName("should clear events after pull")
        void shouldClearEventsAfterPull() {
            Product product = Product.create(
                ProductCode.of("P000001"),
                "Test Product",
                Money.of(100.00),
                "Electronics",
                "Description",
                TENANT_ID,
                CREATED_BY
            );

            List<DomainEvent> firstPull = product.pullDomainEvents();
            List<DomainEvent> secondPull = product.pullDomainEvents();

            assertEquals(1, firstPull.size());
            assertTrue(secondPull.isEmpty());
        }

        @Test
        @DisplayName("should accumulate multiple events")
        void shouldAccumulateMultipleEvents() {
            Product product = Product.create(
                ProductCode.of("P000001"),
                "Test Product",
                Money.of(100.00),
                "Electronics",
                "Description",
                TENANT_ID,
                CREATED_BY
            );
            product.update("New Name", null, null, null, "updater");
            product.changePrice(Money.of(200.00), "price-admin");

            List<DomainEvent> events = product.pullDomainEvents();

            assertEquals(3, events.size());
            assertInstanceOf(ProductCreated.class, events.get(0));
            assertInstanceOf(ProductUpdated.class, events.get(1));
            assertInstanceOf(ProductPriceChanged.class, events.get(2));
        }
    }
}
