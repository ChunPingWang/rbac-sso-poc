package com.example.audit.application.dto;

import com.example.audit.domain.model.AuditLog;
import com.example.audit.domain.model.AuditResult;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for audit log query responses.
 */
public record AuditLogView(
        UUID id,
        Instant timestamp,
        String eventType,
        String aggregateType,
        String aggregateId,
        String username,
        String serviceName,
        String action,
        String payload,
        AuditResult result,
        String errorMessage,
        String clientIp,
        String correlationId,
        boolean payloadTruncated
) {

    /**
     * Creates an AuditLogView from a domain AuditLog.
     *
     * @param auditLog the domain audit log
     * @return the view DTO
     */
    public static AuditLogView from(AuditLog auditLog) {
        return new AuditLogView(
                auditLog.id().value(),
                auditLog.timestamp(),
                auditLog.eventType().value(),
                auditLog.aggregateType(),
                auditLog.aggregateId(),
                auditLog.username(),
                auditLog.serviceName(),
                auditLog.action(),
                auditLog.payload(),
                auditLog.result(),
                auditLog.errorMessage(),
                auditLog.clientIp(),
                auditLog.correlationId(),
                auditLog.isPayloadTruncated()
        );
    }
}
