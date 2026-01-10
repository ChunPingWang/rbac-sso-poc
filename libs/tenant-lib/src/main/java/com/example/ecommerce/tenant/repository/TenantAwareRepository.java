package com.example.ecommerce.tenant.repository;

import com.example.ecommerce.tenant.context.TenantContext;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * 租戶感知的 Repository 實作
 * 自動套用租戶過濾條件
 */
public class TenantAwareRepository<T, ID extends Serializable>
        extends SimpleJpaRepository<T, ID> {

    private final EntityManager entityManager;

    public TenantAwareRepository(JpaEntityInformation<T, ?> entityInformation,
                                  EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
    }

    @Override
    public Optional<T> findById(ID id) {
        enableTenantFilter();
        return super.findById(id);
    }

    @Override
    public List<T> findAll() {
        enableTenantFilter();
        return super.findAll();
    }

    private void enableTenantFilter() {
        String currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant != null && !"system".equals(currentTenant)) {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("tenantFilter")
                   .setParameter("tenantId", currentTenant);
        }
    }
}
