package com.example.audit.infrastructure.persistence;

import com.example.audit.domain.model.AuditResult;
import com.example.audit.infrastructure.persistence.entity.AuditLogJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for audit log entities.
 *
 * <p>Note: This interface intentionally does NOT extend any interfaces that
 * would provide update or delete operations to maintain append-only semantics.</p>
 */
@Repository
public interface SpringDataAuditLogRepository extends JpaRepository<AuditLogJpaEntity, UUID> {

    Page<AuditLogJpaEntity> findByUsername(String username, Pageable pageable);

    Page<AuditLogJpaEntity> findByAggregateTypeAndAggregateId(
            String aggregateType, String aggregateId, Pageable pageable);

    Page<AuditLogJpaEntity> findByEventType(String eventType, Pageable pageable);

    Page<AuditLogJpaEntity> findByTimestampBetween(Instant startTime, Instant endTime, Pageable pageable);

    Page<AuditLogJpaEntity> findByServiceName(String serviceName, Pageable pageable);

    List<AuditLogJpaEntity> findByCorrelationId(String correlationId);

    Page<AuditLogJpaEntity> findByResult(AuditResult result, Pageable pageable);

    @Query("SELECT a FROM AuditLogJpaEntity a WHERE " +
            "(:username IS NULL OR a.username = :username) AND " +
            "(:eventType IS NULL OR a.eventType = :eventType) AND " +
            "(:aggregateType IS NULL OR a.aggregateType = :aggregateType) AND " +
            "(:aggregateId IS NULL OR a.aggregateId = :aggregateId) AND " +
            "(:serviceName IS NULL OR a.serviceName = :serviceName) AND " +
            "(:result IS NULL OR a.result = :result) AND " +
            "(:startTime IS NULL OR a.timestamp >= :startTime) AND " +
            "(:endTime IS NULL OR a.timestamp < :endTime)")
    Page<AuditLogJpaEntity> findByFilters(
            @Param("username") String username,
            @Param("eventType") String eventType,
            @Param("aggregateType") String aggregateType,
            @Param("aggregateId") String aggregateId,
            @Param("serviceName") String serviceName,
            @Param("result") AuditResult result,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);

    long countByResult(AuditResult result);
}
