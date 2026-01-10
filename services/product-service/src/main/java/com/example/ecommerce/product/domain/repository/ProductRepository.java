package com.example.ecommerce.product.domain.repository;

import com.example.ecommerce.product.domain.model.aggregate.Product;
import com.example.ecommerce.product.domain.model.valueobject.*;

import java.util.List;
import java.util.Optional;

/**
 * Product Repository Interface (Output Port)
 * 定義在 Domain Layer，由 Adapter Layer 實作
 */
public interface ProductRepository {
    Optional<Product> findById(ProductId id);
    Optional<Product> findByProductCode(ProductCode code);
    List<Product> findAll();
    List<Product> findByTenantId(String tenantId);
    List<Product> findByCategory(String category);
    Product save(Product product);
    void delete(ProductId id);
    boolean existsByProductCode(ProductCode code);
    long countByTenantId(String tenantId);
}
