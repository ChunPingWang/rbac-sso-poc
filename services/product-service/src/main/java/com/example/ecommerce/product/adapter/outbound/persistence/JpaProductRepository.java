package com.example.ecommerce.product.adapter.outbound.persistence;

import com.example.ecommerce.product.adapter.outbound.persistence.entity.ProductJpaEntity;
import com.example.ecommerce.product.adapter.outbound.persistence.mapper.ProductMapper;
import com.example.ecommerce.product.domain.model.aggregate.Product;
import com.example.ecommerce.product.domain.model.valueobject.*;
import com.example.ecommerce.product.domain.repository.ProductRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class JpaProductRepository implements ProductRepository {

    private final SpringDataProductRepository jpaRepo;
    private final ProductMapper mapper;

    public JpaProductRepository(SpringDataProductRepository jpaRepo, ProductMapper mapper) {
        this.jpaRepo = jpaRepo;
        this.mapper = mapper;
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        return jpaRepo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Product> findByProductCode(ProductCode code) {
        return jpaRepo.findByProductCode(code.value()).map(mapper::toDomain);
    }

    @Override
    public List<Product> findAll() {
        return jpaRepo.findAll().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Product> findByTenantId(String tenantId) {
        return jpaRepo.findByTenantId(tenantId).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Product> findByCategory(String category) {
        return jpaRepo.findByCategory(category).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Product save(Product product) {
        ProductJpaEntity entity = mapper.toEntity(product);
        ProductJpaEntity saved = jpaRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public void delete(ProductId id) {
        jpaRepo.deleteById(id.value());
    }

    @Override
    public boolean existsByProductCode(ProductCode code) {
        return jpaRepo.existsByProductCode(code.value());
    }

    @Override
    public long countByTenantId(String tenantId) {
        return jpaRepo.countByTenantId(tenantId);
    }
}
