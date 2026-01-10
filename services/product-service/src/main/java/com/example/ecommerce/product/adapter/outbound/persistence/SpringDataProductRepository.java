package com.example.ecommerce.product.adapter.outbound.persistence;

import com.example.ecommerce.product.adapter.outbound.persistence.entity.ProductJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataProductRepository extends JpaRepository<ProductJpaEntity, UUID> {
    Optional<ProductJpaEntity> findByProductCode(String productCode);
    List<ProductJpaEntity> findByTenantId(String tenantId);
    List<ProductJpaEntity> findByCategory(String category);
    boolean existsByProductCode(String productCode);
    long countByTenantId(String tenantId);
}
