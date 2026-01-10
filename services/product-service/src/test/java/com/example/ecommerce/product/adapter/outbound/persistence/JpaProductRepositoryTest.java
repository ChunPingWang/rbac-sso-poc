package com.example.ecommerce.product.adapter.outbound.persistence;

import com.example.ecommerce.product.adapter.outbound.persistence.entity.ProductJpaEntity;
import com.example.ecommerce.product.adapter.outbound.persistence.mapper.ProductMapper;
import com.example.ecommerce.product.domain.model.aggregate.Product;
import com.example.ecommerce.product.domain.model.valueobject.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaProductRepository")
class JpaProductRepositoryTest {

    @Mock
    private SpringDataProductRepository jpaRepo;

    @Mock
    private ProductMapper mapper;

    @InjectMocks
    private JpaProductRepository repository;

    private Product sampleProduct;
    private ProductJpaEntity sampleEntity;
    private UUID sampleId;

    @BeforeEach
    void setUp() {
        sampleId = UUID.randomUUID();
        sampleProduct = createSampleProduct(sampleId);
        sampleEntity = createSampleEntity(sampleId);
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return product when found")
        void shouldReturnProductWhenFound() {
            when(jpaRepo.findById(sampleId)).thenReturn(Optional.of(sampleEntity));
            when(mapper.toDomain(sampleEntity)).thenReturn(sampleProduct);

            Optional<Product> result = repository.findById(ProductId.of(sampleId));

            assertTrue(result.isPresent());
            assertEquals(sampleProduct, result.get());
            verify(jpaRepo).findById(sampleId);
            verify(mapper).toDomain(sampleEntity);
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            when(jpaRepo.findById(sampleId)).thenReturn(Optional.empty());

            Optional<Product> result = repository.findById(ProductId.of(sampleId));

            assertFalse(result.isPresent());
            verify(jpaRepo).findById(sampleId);
            verify(mapper, never()).toDomain(any());
        }
    }

    @Nested
    @DisplayName("findByProductCode")
    class FindByProductCode {

        @Test
        @DisplayName("should return product when found by code")
        void shouldReturnProductWhenFoundByCode() {
            String code = "P000001";
            when(jpaRepo.findByProductCode(code)).thenReturn(Optional.of(sampleEntity));
            when(mapper.toDomain(sampleEntity)).thenReturn(sampleProduct);

            Optional<Product> result = repository.findByProductCode(ProductCode.of(code));

            assertTrue(result.isPresent());
            assertEquals(sampleProduct, result.get());
        }

        @Test
        @DisplayName("should return empty when not found by code")
        void shouldReturnEmptyWhenNotFoundByCode() {
            when(jpaRepo.findByProductCode("P999999")).thenReturn(Optional.empty());

            Optional<Product> result = repository.findByProductCode(ProductCode.of("P999999"));

            assertFalse(result.isPresent());
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return all products")
        void shouldReturnAllProducts() {
            ProductJpaEntity entity2 = createSampleEntity(UUID.randomUUID());
            Product product2 = createSampleProduct(entity2.getId());

            when(jpaRepo.findAll()).thenReturn(Arrays.asList(sampleEntity, entity2));
            when(mapper.toDomain(sampleEntity)).thenReturn(sampleProduct);
            when(mapper.toDomain(entity2)).thenReturn(product2);

            List<Product> result = repository.findAll();

            assertEquals(2, result.size());
            verify(jpaRepo).findAll();
        }

        @Test
        @DisplayName("should return empty list when no products")
        void shouldReturnEmptyListWhenNoProducts() {
            when(jpaRepo.findAll()).thenReturn(Collections.emptyList());

            List<Product> result = repository.findAll();

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("findByTenantId")
    class FindByTenantId {

        @Test
        @DisplayName("should return products for tenant")
        void shouldReturnProductsForTenant() {
            when(jpaRepo.findByTenantId("tenant-1")).thenReturn(Collections.singletonList(sampleEntity));
            when(mapper.toDomain(sampleEntity)).thenReturn(sampleProduct);

            List<Product> result = repository.findByTenantId("tenant-1");

            assertEquals(1, result.size());
            verify(jpaRepo).findByTenantId("tenant-1");
        }

        @Test
        @DisplayName("should return empty for non-existent tenant")
        void shouldReturnEmptyForNonExistentTenant() {
            when(jpaRepo.findByTenantId("unknown-tenant")).thenReturn(Collections.emptyList());

            List<Product> result = repository.findByTenantId("unknown-tenant");

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("findByCategory")
    class FindByCategory {

        @Test
        @DisplayName("should return products in category")
        void shouldReturnProductsInCategory() {
            when(jpaRepo.findByCategory("Electronics")).thenReturn(Collections.singletonList(sampleEntity));
            when(mapper.toDomain(sampleEntity)).thenReturn(sampleProduct);

            List<Product> result = repository.findByCategory("Electronics");

            assertEquals(1, result.size());
            verify(jpaRepo).findByCategory("Electronics");
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should save and return product")
        void shouldSaveAndReturnProduct() {
            when(mapper.toEntity(sampleProduct)).thenReturn(sampleEntity);
            when(jpaRepo.save(sampleEntity)).thenReturn(sampleEntity);
            when(mapper.toDomain(sampleEntity)).thenReturn(sampleProduct);

            Product result = repository.save(sampleProduct);

            assertNotNull(result);
            assertEquals(sampleProduct, result);
            verify(mapper).toEntity(sampleProduct);
            verify(jpaRepo).save(sampleEntity);
            verify(mapper).toDomain(sampleEntity);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete by id")
        void shouldDeleteById() {
            doNothing().when(jpaRepo).deleteById(sampleId);

            repository.delete(ProductId.of(sampleId));

            verify(jpaRepo).deleteById(sampleId);
        }
    }

    @Nested
    @DisplayName("existsByProductCode")
    class ExistsByProductCode {

        @Test
        @DisplayName("should return true when product code exists")
        void shouldReturnTrueWhenProductCodeExists() {
            when(jpaRepo.existsByProductCode("P000001")).thenReturn(true);

            boolean result = repository.existsByProductCode(ProductCode.of("P000001"));

            assertTrue(result);
        }

        @Test
        @DisplayName("should return false when product code does not exist")
        void shouldReturnFalseWhenProductCodeDoesNotExist() {
            when(jpaRepo.existsByProductCode("P888888")).thenReturn(false);

            boolean result = repository.existsByProductCode(ProductCode.of("P888888"));

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("countByTenantId")
    class CountByTenantId {

        @Test
        @DisplayName("should return count for tenant")
        void shouldReturnCountForTenant() {
            when(jpaRepo.countByTenantId("tenant-1")).thenReturn(5L);

            long count = repository.countByTenantId("tenant-1");

            assertEquals(5L, count);
        }

        @Test
        @DisplayName("should return zero for empty tenant")
        void shouldReturnZeroForEmptyTenant() {
            when(jpaRepo.countByTenantId("empty-tenant")).thenReturn(0L);

            long count = repository.countByTenantId("empty-tenant");

            assertEquals(0L, count);
        }
    }

    private Product createSampleProduct(UUID id) {
        return Product.create(
            ProductCode.of("P000001"),
            "Test Product",
            Money.of(99.99),
            "Electronics",
            "Description",
            "tenant-1",
            "creator"
        );
    }

    private ProductJpaEntity createSampleEntity(UUID id) {
        ProductJpaEntity entity = new ProductJpaEntity();
        entity.setId(id);
        entity.setProductCode("P000001");
        entity.setName("Test Product");
        entity.setPrice(BigDecimal.valueOf(99.99));
        entity.setCategory("Electronics");
        entity.setDescription("Description");
        entity.setStatus("ACTIVE");
        entity.setTenantId("tenant-1");
        entity.setCreatedBy("creator");
        entity.setCreatedAt(Instant.now());
        return entity;
    }
}
