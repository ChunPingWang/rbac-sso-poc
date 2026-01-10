package com.example.ecommerce.product.application.service;

import com.example.ecommerce.common.dto.PagedResult;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.product.application.dto.ProductView;
import com.example.ecommerce.product.application.port.input.query.*;
import com.example.ecommerce.product.domain.model.aggregate.Product;
import com.example.ecommerce.product.domain.model.valueobject.ProductId;
import com.example.ecommerce.product.domain.repository.ProductRepository;
import com.example.ecommerce.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProductQueryService {

    private final ProductRepository productRepository;

    public ProductQueryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductView handle(GetProductByIdQuery query) {
        Product product = productRepository.findById(ProductId.of(query.productId()))
            .orElseThrow(() -> new ResourceNotFoundException("Product", query.productId()));
        return toView(product);
    }

    public PagedResult<ProductView> handle(ListProductsQuery query) {
        String tenantId = TenantContext.getCurrentTenant();
        List<Product> products;

        if ("system".equals(tenantId)) {
            // 系統管理員可以看到所有商品
            products = query.category() != null
                ? productRepository.findByCategory(query.category())
                : productRepository.findAll();
        } else {
            // 一般使用者只能看到自己租戶的商品
            products = productRepository.findByTenantId(tenantId);
            if (query.category() != null) {
                products = products.stream()
                    .filter(p -> query.category().equals(p.getCategory()))
                    .collect(Collectors.toList());
            }
        }

        // 過濾只顯示 ACTIVE 商品
        products = products.stream()
            .filter(Product::isActive)
            .collect(Collectors.toList());

        // 簡單分頁處理
        int start = query.page() * query.size();
        int end = Math.min(start + query.size(), products.size());
        List<ProductView> pageContent = products.subList(Math.min(start, products.size()), end)
            .stream()
            .map(this::toView)
            .collect(Collectors.toList());

        return new PagedResult<>(pageContent, query.page(), query.size(), products.size());
    }

    private ProductView toView(Product product) {
        return new ProductView(
            product.getId().value(),
            product.getProductCode().value(),
            product.getName(),
            product.getPrice().amount(),
            product.getCategory(),
            product.getDescription(),
            product.getStatus().name(),
            product.getTenantId(),
            product.getCreatedBy(),
            product.getCreatedAt(),
            product.getUpdatedBy(),
            product.getUpdatedAt()
        );
    }
}
