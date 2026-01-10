package com.example.ecommerce.product.application.service;

import com.example.audit.annotation.Auditable;
import com.example.ecommerce.common.exception.BusinessException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.product.application.port.input.command.*;
import com.example.ecommerce.product.domain.model.aggregate.Product;
import com.example.ecommerce.product.domain.model.valueobject.*;
import com.example.ecommerce.product.domain.repository.ProductRepository;
import com.example.ecommerce.security.util.SecurityUtils;
import com.example.ecommerce.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ProductCommandService {

    private final ProductRepository productRepository;

    public ProductCommandService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Auditable(eventType = "PRODUCT_CREATED", resourceType = "Product")
    public UUID handle(CreateProductCommand cmd) {
        ProductCode code = cmd.productCode() != null && !cmd.productCode().isBlank()
            ? ProductCode.of(cmd.productCode())
            : ProductCode.generate();

        if (productRepository.existsByProductCode(code)) {
            throw new BusinessException("Product code already exists: " + code.value(), "DUPLICATE_PRODUCT_CODE");
        }

        String currentUser = SecurityUtils.getCurrentUsername().orElse("system");
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }

        Product product = Product.create(
            code,
            cmd.name(),
            Money.of(cmd.price()),
            cmd.category(),
            cmd.description(),
            tenantId,
            currentUser
        );

        productRepository.save(product);
        return product.getId().value();
    }

    @Auditable(eventType = "PRODUCT_UPDATED", resourceType = "Product")
    public void handle(UpdateProductCommand cmd) {
        Product product = productRepository.findById(ProductId.of(cmd.productId()))
            .orElseThrow(() -> new ResourceNotFoundException("Product", cmd.productId()));

        String currentUser = SecurityUtils.getCurrentUsername().orElse("system");

        Money price = cmd.price() != null ? Money.of(cmd.price()) : null;
        product.update(cmd.name(), price, cmd.category(), cmd.description(), currentUser);

        productRepository.save(product);
    }

    @Auditable(eventType = "PRODUCT_DELETED", resourceType = "Product")
    public void handle(DeleteProductCommand cmd) {
        Product product = productRepository.findById(ProductId.of(cmd.productId()))
            .orElseThrow(() -> new ResourceNotFoundException("Product", cmd.productId()));

        String currentUser = SecurityUtils.getCurrentUsername().orElse("system");
        product.delete(currentUser);

        productRepository.save(product);
    }
}
