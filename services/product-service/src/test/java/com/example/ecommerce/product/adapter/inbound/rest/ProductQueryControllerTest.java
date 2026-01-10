package com.example.ecommerce.product.adapter.inbound.rest;

import com.example.ecommerce.common.dto.PagedResult;
import com.example.ecommerce.product.application.dto.ProductView;
import com.example.ecommerce.product.application.port.input.query.*;
import com.example.ecommerce.product.application.service.ProductQueryService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductQueryController.class)
@DisplayName("ProductQueryController")
class ProductQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductQueryService queryService;

    @Nested
    @DisplayName("GET /api/products/{id}")
    class GetProductById {

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            UUID id = UUID.randomUUID();
            mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = {"USER"})
        @DisplayName("should return product when authenticated")
        void shouldReturnProductWhenAuthenticated() throws Exception {
            UUID id = UUID.randomUUID();
            ProductView product = createProductView(id, "P000001", "Test Product");

            when(queryService.handle(any(GetProductByIdQuery.class))).thenReturn(product);

            mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.name").value("Test Product"))
                .andExpect(jsonPath("$.data.productCode").value("P000001"));

            verify(queryService).handle(any(GetProductByIdQuery.class));
        }

        @Test
        @WithMockUser(roles = {"ADMIN"})
        @DisplayName("should return product for admin")
        void shouldReturnProductForAdmin() throws Exception {
            UUID id = UUID.randomUUID();
            ProductView product = createProductView(id, "P000002", "Admin Product");

            when(queryService.handle(any(GetProductByIdQuery.class))).thenReturn(product);

            mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Admin Product"));
        }
    }

    @Nested
    @DisplayName("GET /api/products")
    class ListProducts {

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = {"USER"})
        @DisplayName("should return product list with default pagination")
        void shouldReturnProductListWithDefaultPagination() throws Exception {
            List<ProductView> products = Arrays.asList(
                createProductView(UUID.randomUUID(), "P000001", "Product A"),
                createProductView(UUID.randomUUID(), "P000002", "Product B")
            );
            PagedResult<ProductView> pagedResult = new PagedResult<>(products, 0, 20, 2);

            when(queryService.handle(any(ListProductsQuery.class))).thenReturn(pagedResult);

            mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20));

            verify(queryService).handle(any(ListProductsQuery.class));
        }

        @Test
        @WithMockUser(roles = {"USER"})
        @DisplayName("should support pagination parameters")
        void shouldSupportPaginationParameters() throws Exception {
            List<ProductView> products = Collections.singletonList(
                createProductView(UUID.randomUUID(), "P000001", "Product A")
            );
            PagedResult<ProductView> pagedResult = new PagedResult<>(products, 1, 10, 15);

            when(queryService.handle(any(ListProductsQuery.class))).thenReturn(pagedResult);

            mockMvc.perform(get("/api/products")
                    .param("page", "1")
                    .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(15));
        }

        @Test
        @WithMockUser(roles = {"USER"})
        @DisplayName("should support category filter")
        void shouldSupportCategoryFilter() throws Exception {
            List<ProductView> products = Collections.singletonList(
                createProductView(UUID.randomUUID(), "P000001", "Electronics Product")
            );
            PagedResult<ProductView> pagedResult = new PagedResult<>(products, 0, 20, 1);

            when(queryService.handle(any(ListProductsQuery.class))).thenReturn(pagedResult);

            mockMvc.perform(get("/api/products")
                    .param("category", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
        }

        @Test
        @WithMockUser(roles = {"USER"})
        @DisplayName("should support sorting parameters")
        void shouldSupportSortingParameters() throws Exception {
            List<ProductView> products = Arrays.asList(
                createProductView(UUID.randomUUID(), "P000001", "Product A"),
                createProductView(UUID.randomUUID(), "P000002", "Product B")
            );
            PagedResult<ProductView> pagedResult = new PagedResult<>(products, 0, 20, 2);

            when(queryService.handle(any(ListProductsQuery.class))).thenReturn(pagedResult);

            mockMvc.perform(get("/api/products")
                    .param("sortBy", "name")
                    .param("sortDirection", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser(roles = {"USER"})
        @DisplayName("should return empty list when no products")
        void shouldReturnEmptyListWhenNoProducts() throws Exception {
            PagedResult<ProductView> pagedResult = new PagedResult<>(Collections.emptyList(), 0, 20, 0);

            when(queryService.handle(any(ListProductsQuery.class))).thenReturn(pagedResult);

            mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(0));
        }
    }

    private ProductView createProductView(UUID id, String code, String name) {
        return new ProductView(
            id,
            code,
            name,
            BigDecimal.valueOf(99.99),
            "Electronics",
            "Description",
            "ACTIVE",
            "tenant-1",
            "creator",
            Instant.now(),
            "updater",
            Instant.now()
        );
    }
}
