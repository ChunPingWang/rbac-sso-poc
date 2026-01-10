package com.example.ecommerce.product.application.service;

import com.example.ecommerce.common.exception.BusinessException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.product.application.port.input.command.*;
import com.example.ecommerce.product.domain.model.aggregate.Product;
import com.example.ecommerce.product.domain.model.valueobject.*;
import com.example.ecommerce.product.domain.repository.ProductRepository;
import com.example.ecommerce.security.util.SecurityUtils;
import com.example.ecommerce.tenant.context.TenantContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductCommandService")
class ProductCommandServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductCommandService commandService;

    private MockedStatic<SecurityUtils> securityUtilsMock;
    private MockedStatic<TenantContext> tenantContextMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        tenantContextMock = mockStatic(TenantContext.class);

        securityUtilsMock.when(SecurityUtils::getCurrentUsername).thenReturn(Optional.of("testuser"));
        tenantContextMock.when(TenantContext::getCurrentTenant).thenReturn("tenant-1");
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
        tenantContextMock.close();
    }

    @Nested
    @DisplayName("Create Product")
    class CreateProduct {

        @Test
        @DisplayName("should create product with provided code")
        void shouldCreateProductWithProvidedCode() {
            CreateProductCommand cmd = new CreateProductCommand(
                "P000001", "Test Product", BigDecimal.valueOf(100.00), "Electronics", "Description"
            );
            when(productRepository.existsByProductCode(any())).thenReturn(false);
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UUID result = commandService.handle(cmd);

            assertNotNull(result);
            verify(productRepository).existsByProductCode(ProductCode.of("P000001"));
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("should generate code when not provided")
        void shouldGenerateCodeWhenNotProvided() {
            CreateProductCommand cmd = new CreateProductCommand(
                null, "Test Product", BigDecimal.valueOf(100.00), "Electronics", "Description"
            );
            when(productRepository.existsByProductCode(any())).thenReturn(false);
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UUID result = commandService.handle(cmd);

            assertNotNull(result);
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("should generate code when blank")
        void shouldGenerateCodeWhenBlank() {
            CreateProductCommand cmd = new CreateProductCommand(
                "  ", "Test Product", BigDecimal.valueOf(100.00), "Electronics", "Description"
            );
            when(productRepository.existsByProductCode(any())).thenReturn(false);
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UUID result = commandService.handle(cmd);

            assertNotNull(result);
        }

        @Test
        @DisplayName("should throw when product code exists")
        void shouldThrowWhenProductCodeExists() {
            CreateProductCommand cmd = new CreateProductCommand(
                "P000001", "Test Product", BigDecimal.valueOf(100.00), "Electronics", "Description"
            );
            when(productRepository.existsByProductCode(ProductCode.of("P000001"))).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class,
                () -> commandService.handle(cmd));

            assertEquals("DUPLICATE_PRODUCT_CODE", ex.getErrorCode());
            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("should use default tenant when not set")
        void shouldUseDefaultTenantWhenNotSet() {
            tenantContextMock.when(TenantContext::getCurrentTenant).thenReturn(null);

            CreateProductCommand cmd = new CreateProductCommand(
                "P000001", "Test Product", BigDecimal.valueOf(100.00), "Electronics", "Description"
            );
            when(productRepository.existsByProductCode(any())).thenReturn(false);
            when(productRepository.save(any())).thenAnswer(inv -> {
                Product p = inv.getArgument(0);
                assertEquals("default", p.getTenantId());
                return p;
            });

            commandService.handle(cmd);

            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("should use system user when not authenticated")
        void shouldUseSystemUserWhenNotAuthenticated() {
            securityUtilsMock.when(SecurityUtils::getCurrentUsername).thenReturn(Optional.empty());

            CreateProductCommand cmd = new CreateProductCommand(
                "P000001", "Test Product", BigDecimal.valueOf(100.00), "Electronics", "Description"
            );
            when(productRepository.existsByProductCode(any())).thenReturn(false);
            when(productRepository.save(any())).thenAnswer(inv -> {
                Product p = inv.getArgument(0);
                assertEquals("system", p.getCreatedBy());
                return p;
            });

            commandService.handle(cmd);

            verify(productRepository).save(any(Product.class));
        }
    }

    @Nested
    @DisplayName("Update Product")
    class UpdateProduct {

        @Test
        @DisplayName("should update product")
        void shouldUpdateProduct() {
            UUID id = UUID.randomUUID();
            Product product = createTestProduct();

            when(productRepository.findById(ProductId.of(id))).thenReturn(Optional.of(product));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateProductCommand cmd = new UpdateProductCommand(
                id, "New Name", BigDecimal.valueOf(200.00), "Clothing", "New Description"
            );

            assertDoesNotThrow(() -> commandService.handle(cmd));

            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("should throw when product not found")
        void shouldThrowWhenProductNotFound() {
            UUID id = UUID.randomUUID();
            when(productRepository.findById(ProductId.of(id))).thenReturn(Optional.empty());

            UpdateProductCommand cmd = new UpdateProductCommand(
                id, "New Name", null, null, null
            );

            assertThrows(ResourceNotFoundException.class,
                () -> commandService.handle(cmd));
        }

        @Test
        @DisplayName("should allow partial update")
        void shouldAllowPartialUpdate() {
            UUID id = UUID.randomUUID();
            Product product = createTestProduct();

            when(productRepository.findById(ProductId.of(id))).thenReturn(Optional.of(product));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateProductCommand cmd = new UpdateProductCommand(
                id, "New Name", null, null, null
            );

            assertDoesNotThrow(() -> commandService.handle(cmd));
            verify(productRepository).save(any(Product.class));
        }

        private Product createTestProduct() {
            return Product.create(
                ProductCode.of("P000001"),
                "Original Name",
                Money.of(100.00),
                "Electronics",
                "Description",
                "tenant-1",
                "creator"
            );
        }
    }

    @Nested
    @DisplayName("Delete Product")
    class DeleteProduct {

        @Test
        @DisplayName("should delete product")
        void shouldDeleteProduct() {
            UUID id = UUID.randomUUID();
            Product product = Product.create(
                ProductCode.of("P000001"),
                "Test Product",
                Money.of(100.00),
                "Electronics",
                "Description",
                "tenant-1",
                "creator"
            );

            when(productRepository.findById(ProductId.of(id))).thenReturn(Optional.of(product));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DeleteProductCommand cmd = new DeleteProductCommand(id);

            assertDoesNotThrow(() -> commandService.handle(cmd));

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(captor.capture());
            assertEquals(ProductStatus.DELETED, captor.getValue().getStatus());
        }

        @Test
        @DisplayName("should throw when product not found")
        void shouldThrowWhenProductNotFound() {
            UUID id = UUID.randomUUID();
            when(productRepository.findById(ProductId.of(id))).thenReturn(Optional.empty());

            DeleteProductCommand cmd = new DeleteProductCommand(id);

            assertThrows(ResourceNotFoundException.class,
                () -> commandService.handle(cmd));
        }
    }
}
