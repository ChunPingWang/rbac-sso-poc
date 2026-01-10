package com.example.ecommerce.product.adapter.inbound.rest;

import com.example.ecommerce.product.application.port.input.command.*;
import com.example.ecommerce.product.application.service.ProductCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductCommandController.class)
@EnableMethodSecurity
@DisplayName("ProductCommandController")
class ProductCommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductCommandService commandService;

    @Nested
    @DisplayName("POST /api/products")
    class CreateProduct {

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(post("/api/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Test\",\"price\":100}")
                    .with(csrf()))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = {"USER"})
        @DisplayName("should return 403 when user role insufficient")
        void shouldReturn403WhenUserRoleInsufficient() throws Exception {
            mockMvc.perform(post("/api/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Test\",\"price\":100,\"category\":\"Electronics\"}")
                    .with(csrf()))
                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = {"ADMIN"})
        @DisplayName("should create product when admin")
        void shouldCreateProductWhenAdmin() throws Exception {
            UUID productId = UUID.randomUUID();
            when(commandService.handle(any(CreateProductCommand.class))).thenReturn(productId);

            String requestBody = """
                {
                    "productCode": "P000001",
                    "name": "Test Product",
                    "price": 99.99,
                    "category": "Electronics",
                    "description": "A test product"
                }
                """;

            mockMvc.perform(post("/api/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(productId.toString()));

            verify(commandService).handle(any(CreateProductCommand.class));
        }

        @Test
        @WithMockUser(roles = {"TENANT_ADMIN"})
        @DisplayName("should create product when tenant admin")
        void shouldCreateProductWhenTenantAdmin() throws Exception {
            UUID productId = UUID.randomUUID();
            when(commandService.handle(any(CreateProductCommand.class))).thenReturn(productId);

            String requestBody = """
                {
                    "name": "Test Product",
                    "price": 50.00,
                    "category": "Books"
                }
                """;

            mockMvc.perform(post("/api/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("PUT /api/products/{id}")
    class UpdateProduct {

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            UUID id = UUID.randomUUID();
            mockMvc.perform(put("/api/products/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Updated\"}")
                    .with(csrf()))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = {"USER"})
        @DisplayName("should return 403 when user role insufficient")
        void shouldReturn403WhenUserRoleInsufficient() throws Exception {
            UUID id = UUID.randomUUID();
            mockMvc.perform(put("/api/products/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Updated\"}")
                    .with(csrf()))
                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = {"ADMIN"})
        @DisplayName("should update product when admin")
        void shouldUpdateProductWhenAdmin() throws Exception {
            UUID id = UUID.randomUUID();
            doNothing().when(commandService).handle(any(UpdateProductCommand.class));

            String requestBody = """
                {
                    "name": "Updated Product",
                    "price": 149.99,
                    "category": "Electronics",
                    "description": "Updated description"
                }
                """;

            mockMvc.perform(put("/api/products/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product updated successfully"));

            verify(commandService).handle(any(UpdateProductCommand.class));
        }

        @Test
        @WithMockUser(roles = {"TENANT_ADMIN"})
        @DisplayName("should update product when tenant admin")
        void shouldUpdateProductWhenTenantAdmin() throws Exception {
            UUID id = UUID.randomUUID();
            doNothing().when(commandService).handle(any(UpdateProductCommand.class));

            String requestBody = """
                {
                    "name": "Updated by Tenant Admin"
                }
                """;

            mockMvc.perform(put("/api/products/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("DELETE /api/products/{id}")
    class DeleteProduct {

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            UUID id = UUID.randomUUID();
            mockMvc.perform(delete("/api/products/" + id)
                    .with(csrf()))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = {"USER"})
        @DisplayName("should return 403 when user role")
        void shouldReturn403WhenUserRole() throws Exception {
            UUID id = UUID.randomUUID();
            mockMvc.perform(delete("/api/products/" + id)
                    .with(csrf()))
                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = {"TENANT_ADMIN"})
        @DisplayName("should return 403 when tenant admin")
        void shouldReturn403WhenTenantAdmin() throws Exception {
            UUID id = UUID.randomUUID();
            mockMvc.perform(delete("/api/products/" + id)
                    .with(csrf()))
                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = {"ADMIN"})
        @DisplayName("should delete product when admin")
        void shouldDeleteProductWhenAdmin() throws Exception {
            UUID id = UUID.randomUUID();
            doNothing().when(commandService).handle(any(DeleteProductCommand.class));

            mockMvc.perform(delete("/api/products/" + id)
                    .with(csrf()))
                .andExpect(status().isNoContent());

            verify(commandService).handle(any(DeleteProductCommand.class));
        }
    }
}
