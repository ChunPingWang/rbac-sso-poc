package com.example.audit.infrastructure.persistence.mapper;

import com.example.audit.domain.model.AuditEventType;
import com.example.audit.domain.model.AuditLog;
import com.example.audit.domain.model.AuditLogId;
import com.example.audit.infrastructure.persistence.entity.AuditLogJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper between domain AuditLog and JPA entity.
 */
@Component
public class AuditLogMapper {

    /**
     * Converts a domain AuditLog to a JPA entity.
     *
     * @param domain the domain audit log
     * @return the JPA entity
     */
    public AuditLogJpaEntity toEntity(AuditLog domain) {
        if (domain == null) {
            return null;
        }

        return new AuditLogJpaEntity(
                domain.id().value(),
                domain.timestamp(),
                domain.eventType().value(),
                domain.aggregateType(),
                domain.aggregateId(),
                domain.username(),
                domain.serviceName(),
                domain.action(),
                domain.payload(),
                domain.result(),
                domain.errorMessage(),
                domain.clientIp(),
                domain.correlationId(),
                domain.isPayloadTruncated()
        );
    }

    /**
     * Converts a JPA entity to a domain AuditLog.
     *
     * @param entity the JPA entity
     * @return the domain audit log
     */
    public AuditLog toDomain(AuditLogJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return AuditLog.builder()
                .id(AuditLogId.of(entity.getId()))
                .timestamp(entity.getTimestamp())
                .eventType(AuditEventType.of(entity.getEventType()))
                .aggregateType(entity.getAggregateType())
                .aggregateId(entity.getAggregateId())
                .username(entity.getUsername())
                .serviceName(entity.getServiceName())
                .action(entity.getAction())
                .payload(entity.getPayload())
                .result(entity.getResult())
                .errorMessage(entity.getErrorMessage())
                .clientIp(entity.getClientIp())
                .correlationId(entity.getCorrelationId())
                .payloadTruncated(entity.isPayloadTruncated())
                .build();
    }
}
