package com.example.ecommerce.product.adapter.inbound.rest;

import com.example.ecommerce.common.dto.ApiResponse;
import com.example.ecommerce.common.dto.PagedResult;
import com.example.ecommerce.product.application.dto.ProductView;
import com.example.ecommerce.product.application.port.input.query.*;
import com.example.ecommerce.product.application.service.ProductQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product Queries", description = "商品查詢 API")
public class ProductQueryController {

    private final ProductQueryService queryService;

    public ProductQueryController(ProductQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "查詢單一商品", description = "根據 ID 查詢商品詳情")
    public ApiResponse<ProductView> getProduct(@PathVariable UUID id) {
        ProductView product = queryService.handle(new GetProductByIdQuery(id));
        return ApiResponse.success(product);
    }

    @GetMapping
    @Operation(summary = "查詢商品列表", description = "分頁查詢商品列表，可依分類過濾")
    public ApiResponse<PagedResult<ProductView>> listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        ListProductsQuery query = new ListProductsQuery(page, size, category, sortBy, sortDirection);
        PagedResult<ProductView> result = queryService.handle(query);
        return ApiResponse.success(result);
    }
}
