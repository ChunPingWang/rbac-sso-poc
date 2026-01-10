package com.example.ecommerce.product.adapter.outbound.persistence.mapper;

import com.example.ecommerce.product.adapter.outbound.persistence.entity.ProductJpaEntity;
import com.example.ecommerce.product.domain.model.aggregate.Product;
import com.example.ecommerce.product.domain.model.valueobject.*;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProductMapper")
class ProductMapperTest {

    private ProductMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ProductMapper();
    }

    @Nested
    @DisplayName("toDomain")
    class ToDomain {

        @Test
        @DisplayName("should map entity to domain product")
        void shouldMapEntityToDomainProduct() {
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();

            ProductJpaEntity entity = createEntity(id, "P000001", "Test Product", now);

            Product product = mapper.toDomain(entity);

            assertNotNull(product);
            assertEquals(id, product.getId().value());
            assertEquals("P000001", product.getProductCode().value());
            assertEquals("Test Product", product.getName());
            assertEquals(BigDecimal.valueOf(99.99), product.getPrice().amount());
            assertEquals("Electronics", product.getCategory());
            assertEquals("A test product", product.getDescription());
            assertEquals("tenant-1", product.getTenantId());
            assertEquals(ProductStatus.ACTIVE, product.getStatus());
            assertEquals("creator", product.getCreatedBy());
            assertEquals(now, product.getCreatedAt());
        }

        @Test
        @DisplayName("should map entity with INACTIVE status")
        void shouldMapEntityWithInactiveStatus() {
            UUID id = UUID.randomUUID();
            ProductJpaEntity entity = createEntity(id, "P000002", "Inactive Product", Instant.now());
            entity.setStatus("INACTIVE");

            Product product = mapper.toDomain(entity);

            assertEquals(ProductStatus.INACTIVE, product.getStatus());
        }

        @Test
        @DisplayName("should map entity with DELETED status")
        void shouldMapEntityWithDeletedStatus() {
            UUID id = UUID.randomUUID();
            ProductJpaEntity entity = createEntity(id, "P000003", "Deleted Product", Instant.now());
            entity.setStatus("DELETED");

            Product product = mapper.toDomain(entity);

            assertEquals(ProductStatus.DELETED, product.getStatus());
        }

        @Test
        @DisplayName("should preserve all fields during mapping")
        void shouldPreserveAllFieldsDuringMapping() {
            UUID id = UUID.randomUUID();
            Instant createdAt = Instant.now().minusSeconds(3600);
            Instant updatedAt = Instant.now();

            ProductJpaEntity entity = new ProductJpaEntity();
            entity.setId(id);
            entity.setProductCode("P999999");
            entity.setName("Full Test Product");
            entity.setPrice(BigDecimal.valueOf(199.99));
            entity.setCategory("Books");
            entity.setDescription("Full description");
            entity.setStatus("ACTIVE");
            entity.setTenantId("tenant-x");
            entity.setCreatedBy("admin");
            entity.setCreatedAt(createdAt);
            entity.setUpdatedBy("editor");
            entity.setUpdatedAt(updatedAt);

            Product product = mapper.toDomain(entity);

            assertEquals("editor", product.getUpdatedBy());
            assertEquals(updatedAt, product.getUpdatedAt());
            assertEquals("admin", product.getCreatedBy());
            assertEquals(createdAt, product.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("should map domain product to entity")
        void shouldMapDomainProductToEntity() {
            Product product = Product.create(
                ProductCode.of("P000001"),
                "Test Product",
                Money.of(99.99),
                "Electronics",
                "A test product",
                "tenant-1",
                "creator"
            );

            ProductJpaEntity entity = mapper.toEntity(product);

            assertNotNull(entity);
            assertEquals(product.getId().value(), entity.getId());
            assertEquals("P000001", entity.getProductCode());
            assertEquals("Test Product", entity.getName());
            assertEquals(BigDecimal.valueOf(99.99), entity.getPrice());
            assertEquals("Electronics", entity.getCategory());
            assertEquals("A test product", entity.getDescription());
            assertEquals("ACTIVE", entity.getStatus());
            assertEquals("tenant-1", entity.getTenantId());
            assertEquals("creator", entity.getCreatedBy());
        }

        @Test
        @DisplayName("should map inactive product to entity")
        void shouldMapInactiveProductToEntity() {
            Product product = Product.create(
                ProductCode.of("P000002"),
                "Inactive Product",
                Money.of(50.00),
                "Books",
                "Description",
                "tenant-2",
                "admin"
            );
            product.deactivate();

            ProductJpaEntity entity = mapper.toEntity(product);

            assertEquals("INACTIVE", entity.getStatus());
        }

        @Test
        @DisplayName("should map deleted product to entity")
        void shouldMapDeletedProductToEntity() {
            Product product = Product.create(
                ProductCode.of("P000003"),
                "To Delete",
                Money.of(25.00),
                "Clothing",
                "Description",
                "tenant-3",
                "manager"
            );
            product.delete("deleter");

            ProductJpaEntity entity = mapper.toEntity(product);

            assertEquals("DELETED", entity.getStatus());
        }
    }

    @Nested
    @DisplayName("Round Trip")
    class RoundTrip {

        @Test
        @DisplayName("should preserve data through domain to entity to domain conversion")
        void shouldPreserveDataThroughRoundTrip() {
            Product original = Product.create(
                ProductCode.of("P123456"),
                "Round Trip Product",
                Money.of(150.00),
                "Electronics",
                "Testing round trip",
                "tenant-rt",
                "tester"
            );

            ProductJpaEntity entity = mapper.toEntity(original);
            Product restored = mapper.toDomain(entity);

            assertEquals(original.getId().value(), restored.getId().value());
            assertEquals(original.getProductCode().value(), restored.getProductCode().value());
            assertEquals(original.getName(), restored.getName());
            assertEquals(original.getPrice().amount(), restored.getPrice().amount());
            assertEquals(original.getCategory(), restored.getCategory());
            assertEquals(original.getDescription(), restored.getDescription());
            assertEquals(original.getStatus(), restored.getStatus());
            assertEquals(original.getTenantId(), restored.getTenantId());
            assertEquals(original.getCreatedBy(), restored.getCreatedBy());
        }
    }

    private ProductJpaEntity createEntity(UUID id, String code, String name, Instant now) {
        ProductJpaEntity entity = new ProductJpaEntity();
        entity.setId(id);
        entity.setProductCode(code);
        entity.setName(name);
        entity.setPrice(BigDecimal.valueOf(99.99));
        entity.setCategory("Electronics");
        entity.setDescription("A test product");
        entity.setStatus("ACTIVE");
        entity.setTenantId("tenant-1");
        entity.setCreatedBy("creator");
        entity.setCreatedAt(now);
        entity.setUpdatedBy("updater");
        entity.setUpdatedAt(now);
        return entity;
    }
}
