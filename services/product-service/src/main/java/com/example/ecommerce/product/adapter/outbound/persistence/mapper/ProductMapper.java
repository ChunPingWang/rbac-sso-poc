package com.example.ecommerce.product.adapter.outbound.persistence.mapper;

import com.example.ecommerce.product.adapter.outbound.persistence.entity.ProductJpaEntity;
import com.example.ecommerce.product.domain.model.aggregate.Product;
import com.example.ecommerce.product.domain.model.valueobject.*;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toDomain(ProductJpaEntity entity) {
        return new Product(
            ProductId.of(entity.getId()),
            ProductCode.of(entity.getProductCode()),
            entity.getName(),
            Money.of(entity.getPrice()),
            entity.getCategory(),
            entity.getDescription(),
            entity.getTenantId(),
            ProductStatus.valueOf(entity.getStatus()),
            entity.getCreatedBy(),
            entity.getCreatedAt(),
            entity.getUpdatedBy(),
            entity.getUpdatedAt()
        );
    }

    public ProductJpaEntity toEntity(Product product) {
        ProductJpaEntity entity = new ProductJpaEntity();
        entity.setId(product.getId().value());
        entity.setProductCode(product.getProductCode().value());
        entity.setName(product.getName());
        entity.setPrice(product.getPrice().amount());
        entity.setCategory(product.getCategory());
        entity.setDescription(product.getDescription());
        entity.setStatus(product.getStatus().name());
        entity.setTenantId(product.getTenantId());
        entity.setCreatedBy(product.getCreatedBy());
        entity.setCreatedAt(product.getCreatedAt());
        entity.setUpdatedBy(product.getUpdatedBy());
        entity.setUpdatedAt(product.getUpdatedAt());
        return entity;
    }
}
