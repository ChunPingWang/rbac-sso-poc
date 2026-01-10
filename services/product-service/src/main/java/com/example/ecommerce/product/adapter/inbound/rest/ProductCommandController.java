package com.example.ecommerce.product.adapter.inbound.rest;

import com.example.ecommerce.common.dto.ApiResponse;
import com.example.ecommerce.product.adapter.inbound.rest.dto.*;
import com.example.ecommerce.product.application.port.input.command.*;
import com.example.ecommerce.product.application.service.ProductCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product Commands", description = "商品寫入操作 API")
public class ProductCommandController {

    private final ProductCommandService commandService;

    public ProductCommandController(ProductCommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "建立商品", description = "建立新商品，需要 ADMIN 或 TENANT_ADMIN 角色")
    public ApiResponse<UUID> createProduct(@Valid @RequestBody CreateProductRequest request) {
        CreateProductCommand cmd = new CreateProductCommand(
            request.productCode(),
            request.name(),
            request.price(),
            request.category(),
            request.description()
        );

        UUID productId = commandService.handle(cmd);
        return ApiResponse.success(productId, "Product created successfully");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "更新商品", description = "更新商品資訊，需要 ADMIN 或 TENANT_ADMIN 角色")
    public ApiResponse<Void> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {

        UpdateProductCommand cmd = new UpdateProductCommand(
            id,
            request.name(),
            request.price(),
            request.category(),
            request.description()
        );

        commandService.handle(cmd);
        return ApiResponse.success(null, "Product updated successfully");
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "刪除商品", description = "軟刪除商品，僅 ADMIN 可執行")
    public void deleteProduct(@PathVariable UUID id) {
        commandService.handle(new DeleteProductCommand(id));
    }
}
