package com.example.ecommerce.product.application.service;

import com.example.ecommerce.common.dto.PagedResult;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.product.application.dto.ProductView;
import com.example.ecommerce.product.application.port.input.query.*;
import com.example.ecommerce.product.domain.model.aggregate.Product;
import com.example.ecommerce.product.domain.model.valueobject.*;
import com.example.ecommerce.product.domain.repository.ProductRepository;
import com.example.ecommerce.tenant.context.TenantContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductQueryService")
class ProductQueryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductQueryService queryService;

    private MockedStatic<TenantContext> tenantContextMock;

    @BeforeEach
    void setUp() {
        tenantContextMock = mockStatic(TenantContext.class);
    }

    @AfterEach
    void tearDown() {
        tenantContextMock.close();
    }

    @Nested
    @DisplayName("Get Product By ID")
    class GetProductById {

        @Test
        @DisplayName("should return product when found")
        void shouldReturnProductWhenFound() {
            UUID id = UUID.randomUUID();
            Product product = createActiveProduct("P000001", "Test Product", "tenant-1");

            when(productRepository.findById(ProductId.of(id))).thenReturn(Optional.of(product));

            GetProductByIdQuery query = new GetProductByIdQuery(id);
            ProductView result = queryService.handle(query);

            assertNotNull(result);
            assertEquals("Test Product", result.name());
            assertEquals("P000001", result.productCode());
        }

        @Test
        @DisplayName("should throw when product not found")
        void shouldThrowWhenProductNotFound() {
            UUID id = UUID.randomUUID();
            when(productRepository.findById(ProductId.of(id))).thenReturn(Optional.empty());

            GetProductByIdQuery query = new GetProductByIdQuery(id);

            assertThrows(ResourceNotFoundException.class, () -> queryService.handle(query));
        }
    }

    @Nested
    @DisplayName("List Products - System Admin")
    class ListProductsSystemAdmin {

        @BeforeEach
        void setUp() {
            tenantContextMock.when(TenantContext::getCurrentTenant).thenReturn("system");
        }

        @Test
        @DisplayName("should return all active products")
        void shouldReturnAllActiveProducts() {
            List<Product> products = Arrays.asList(
                createActiveProduct("P000001", "Product A", "tenant-1"),
                createActiveProduct("P000002", "Product B", "tenant-2")
            );
            when(productRepository.findAll()).thenReturn(products);

            ListProductsQuery query = new ListProductsQuery(0, 10, null, "createdAt", "DESC");
            PagedResult<ProductView> result = queryService.handle(query);

            assertEquals(2, result.getContent().size());
            assertEquals(2, result.getTotalElements());
        }

        @Test
        @DisplayName("should filter by category")
        void shouldFilterByCategory() {
            List<Product> products = Arrays.asList(
                createActiveProduct("P000001", "Product A", "tenant-1"),
                createActiveProduct("P000002", "Product B", "tenant-2")
            );
            when(productRepository.findByCategory("Electronics")).thenReturn(products);

            ListProductsQuery query = new ListProductsQuery(0, 10, "Electronics", "createdAt", "DESC");
            PagedResult<ProductView> result = queryService.handle(query);

            assertEquals(2, result.getContent().size());
            verify(productRepository).findByCategory("Electronics");
            verify(productRepository, never()).findAll();
        }

        @Test
        @DisplayName("should filter out inactive products")
        void shouldFilterOutInactiveProducts() {
            Product activeProduct = createActiveProduct("P000001", "Active", "tenant-1");
            Product inactiveProduct = createActiveProduct("P000002", "Inactive", "tenant-1");
            inactiveProduct.deactivate();

            when(productRepository.findAll()).thenReturn(Arrays.asList(activeProduct, inactiveProduct));

            ListProductsQuery query = new ListProductsQuery(0, 10, null, "createdAt", "DESC");
            PagedResult<ProductView> result = queryService.handle(query);

            assertEquals(1, result.getContent().size());
            assertEquals("Active", result.getContent().get(0).name());
        }
    }

    @Nested
    @DisplayName("List Products - Tenant User")
    class ListProductsTenantUser {

        @BeforeEach
        void setUp() {
            tenantContextMock.when(TenantContext::getCurrentTenant).thenReturn("tenant-1");
        }

        @Test
        @DisplayName("should return only tenant products")
        void shouldReturnOnlyTenantProducts() {
            List<Product> tenantProducts = Arrays.asList(
                createActiveProduct("P000001", "Product A", "tenant-1"),
                createActiveProduct("P000002", "Product B", "tenant-1")
            );
            when(productRepository.findByTenantId("tenant-1")).thenReturn(tenantProducts);

            ListProductsQuery query = new ListProductsQuery(0, 10, null, "createdAt", "DESC");
            PagedResult<ProductView> result = queryService.handle(query);

            assertEquals(2, result.getContent().size());
            verify(productRepository).findByTenantId("tenant-1");
            verify(productRepository, never()).findAll();
        }

        @Test
        @DisplayName("should filter tenant products by category")
        void shouldFilterTenantProductsByCategory() {
            Product electronics = createActiveProductWithCategory("P000001", "Phone", "tenant-1", "Electronics");
            Product clothing = createActiveProductWithCategory("P000002", "Shirt", "tenant-1", "Clothing");

            when(productRepository.findByTenantId("tenant-1")).thenReturn(Arrays.asList(electronics, clothing));

            ListProductsQuery query = new ListProductsQuery(0, 10, "Electronics", "createdAt", "DESC");
            PagedResult<ProductView> result = queryService.handle(query);

            assertEquals(1, result.getContent().size());
            assertEquals("Phone", result.getContent().get(0).name());
        }
    }

    @Nested
    @DisplayName("Pagination")
    class Pagination {

        @BeforeEach
        void setUp() {
            tenantContextMock.when(TenantContext::getCurrentTenant).thenReturn("system");
        }

        @Test
        @DisplayName("should paginate results")
        void shouldPaginateResults() {
            List<Product> products = new ArrayList<>();
            for (int i = 0; i < 25; i++) {
                products.add(createActiveProduct("P" + String.format("%06d", i), "Product " + i, "tenant-1"));
            }
            when(productRepository.findAll()).thenReturn(products);

            // First page
            ListProductsQuery query1 = new ListProductsQuery(0, 10, null, "createdAt", "DESC");
            PagedResult<ProductView> result1 = queryService.handle(query1);

            assertEquals(10, result1.getContent().size());
            assertEquals(25, result1.getTotalElements());
            assertEquals(0, result1.getPage());
            assertEquals(10, result1.getSize());

            // Second page
            ListProductsQuery query2 = new ListProductsQuery(1, 10, null, "createdAt", "DESC");
            PagedResult<ProductView> result2 = queryService.handle(query2);

            assertEquals(10, result2.getContent().size());
            assertEquals(1, result2.getPage());

            // Last page
            ListProductsQuery query3 = new ListProductsQuery(2, 10, null, "createdAt", "DESC");
            PagedResult<ProductView> result3 = queryService.handle(query3);

            assertEquals(5, result3.getContent().size());
            assertEquals(2, result3.getPage());
        }

        @Test
        @DisplayName("should return empty for page beyond data")
        void shouldReturnEmptyForPageBeyondData() {
            List<Product> products = Arrays.asList(
                createActiveProduct("P000001", "Product A", "tenant-1")
            );
            when(productRepository.findAll()).thenReturn(products);

            ListProductsQuery query = new ListProductsQuery(10, 10, null, "createdAt", "DESC");
            PagedResult<ProductView> result = queryService.handle(query);

            assertTrue(result.getContent().isEmpty());
            assertEquals(1, result.getTotalElements());
        }
    }

    // Helper methods
    private Product createActiveProduct(String code, String name, String tenantId) {
        return Product.create(
            ProductCode.of(code),
            name,
            Money.of(100.00),
            "Electronics",
            "Description",
            tenantId,
            "creator"
        );
    }

    private Product createActiveProductWithCategory(String code, String name, String tenantId, String category) {
        return Product.create(
            ProductCode.of(code),
            name,
            Money.of(100.00),
            category,
            "Description",
            tenantId,
            "creator"
        );
    }
}
