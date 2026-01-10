package com.example.audit.infrastructure.persistence.entity;

import com.example.audit.domain.model.AuditResult;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for audit log persistence.
 *
 * <p>Marked as @Immutable to enforce append-only semantics per FR-011.
 * All columns are set with updatable=false to prevent modifications.</p>
 */
@Entity
@Table(name = "audit_logs")
@Immutable
public class AuditLogJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    @Column(name = "event_type", nullable = false, length = 100, updatable = false)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 100, updatable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", length = 255, updatable = false)
    private String aggregateId;

    @Column(name = "username", nullable = false, length = 100, updatable = false)
    private String username;

    @Column(name = "service_name", nullable = false, length = 100, updatable = false)
    private String serviceName;

    @Column(name = "action", length = 255, updatable = false)
    private String action;

    @Column(name = "payload", columnDefinition = "TEXT", updatable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20, updatable = false)
    private AuditResult result;

    @Column(name = "error_message", columnDefinition = "TEXT", updatable = false)
    private String errorMessage;

    @Column(name = "client_ip", length = 45, updatable = false)
    private String clientIp;

    @Column(name = "correlation_id", length = 100, updatable = false)
    private String correlationId;

    @Column(name = "payload_truncated", nullable = false, updatable = false)
    private boolean payloadTruncated;

    // Protected no-arg constructor for JPA
    protected AuditLogJpaEntity() {
    }

    // All-args constructor for mapping from domain
    public AuditLogJpaEntity(
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
            boolean payloadTruncated) {
        this.id = id;
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.username = username;
        this.serviceName = serviceName;
        this.action = action;
        this.payload = payload;
        this.result = result;
        this.errorMessage = errorMessage;
        this.clientIp = clientIp;
        this.correlationId = correlationId;
        this.payloadTruncated = payloadTruncated;
    }

    // Getters only - no setters (immutable)
    public UUID getId() {
        return id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getUsername() {
        return username;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getAction() {
        return action;
    }

    public String getPayload() {
        return payload;
    }

    public AuditResult getResult() {
        return result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public boolean isPayloadTruncated() {
        return payloadTruncated;
    }
}
