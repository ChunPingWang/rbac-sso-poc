package com.example.audit.infrastructure.persistence;

import com.example.audit.domain.model.AuditEventType;
import com.example.audit.domain.model.AuditLog;
import com.example.audit.domain.model.AuditLogId;
import com.example.audit.domain.port.AuditLogRepository;
import com.example.audit.infrastructure.persistence.entity.AuditLogJpaEntity;
import com.example.audit.infrastructure.persistence.mapper.AuditLogMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JPA implementation of the AuditLogRepository port.
 *
 * <p>Enforces append-only semantics by only implementing save and query operations.
 * Update and delete operations are intentionally not implemented per FR-011.</p>
 */
@Repository
public class JpaAuditLogRepository implements AuditLogRepository {

    private final SpringDataAuditLogRepository jpaRepository;
    private final AuditLogMapper mapper;

    public JpaAuditLogRepository(SpringDataAuditLogRepository jpaRepository, AuditLogMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        AuditLogJpaEntity entity = mapper.toEntity(auditLog);
        AuditLogJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<AuditLog> findById(AuditLogId id) {
        return jpaRepository.findById(id.value())
                .map(mapper::toDomain);
    }

    @Override
    public Page<AuditLog> findByUsername(String username, Pageable pageable) {
        return jpaRepository.findByUsername(username, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<AuditLog> findByAggregateTypeAndAggregateId(
            String aggregateType, String aggregateId, Pageable pageable) {
        return jpaRepository.findByAggregateTypeAndAggregateId(aggregateType, aggregateId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<AuditLog> findByEventType(AuditEventType eventType, Pageable pageable) {
        return jpaRepository.findByEventType(eventType.value(), pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<AuditLog> findByTimestampBetween(Instant startTime, Instant endTime, Pageable pageable) {
        return jpaRepository.findByTimestampBetween(startTime, endTime, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<AuditLog> findByServiceName(String serviceName, Pageable pageable) {
        return jpaRepository.findByServiceName(serviceName, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public List<AuditLog> findByCorrelationId(String correlationId) {
        return jpaRepository.findByCorrelationId(correlationId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsById(AuditLogId id) {
        return jpaRepository.existsById(id.value());
    }
}
