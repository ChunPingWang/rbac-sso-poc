package com.example.audit.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root representing a single audit trail entry.
 * Immutable once created - supports append-only semantics per FR-011.
 */
public final class AuditLog {

    private final AuditLogId id;
    private final Instant timestamp;
    private final AuditEventType eventType;
    private final String aggregateType;
    private final String aggregateId;
    private final String username;
    private final String serviceName;
    private final String action;
    private final String payload;
    private final AuditResult result;
    private final String errorMessage;
    private final String clientIp;
    private final String correlationId;
    private final boolean payloadTruncated;

    private AuditLog(Builder builder) {
        // Required field validation
        Objects.requireNonNull(builder.id, "id must not be null");
        Objects.requireNonNull(builder.timestamp, "timestamp must not be null");
        Objects.requireNonNull(builder.eventType, "eventType must not be null");
        requireNotBlank(builder.aggregateType, "aggregateType");
        requireNotBlank(builder.username, "username");
        requireNotBlank(builder.serviceName, "serviceName");
        Objects.requireNonNull(builder.result, "result must not be null");

        this.id = builder.id;
        this.timestamp = builder.timestamp;
        this.eventType = builder.eventType;
        this.aggregateType = builder.aggregateType;
        this.aggregateId = builder.aggregateId;
        this.username = builder.username;
        this.serviceName = builder.serviceName;
        this.action = builder.action;
        this.payload = builder.payload;
        this.result = builder.result;
        this.errorMessage = builder.errorMessage;
        this.clientIp = builder.clientIp;
        this.correlationId = builder.correlationId;
        this.payloadTruncated = builder.payloadTruncated;
    }

    private static void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters only - immutable
    public AuditLogId id() {
        return id;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public AuditEventType eventType() {
        return eventType;
    }

    public String aggregateType() {
        return aggregateType;
    }

    public String aggregateId() {
        return aggregateId;
    }

    public String username() {
        return username;
    }

    public String serviceName() {
        return serviceName;
    }

    public String action() {
        return action;
    }

    public String payload() {
        return payload;
    }

    public AuditResult result() {
        return result;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public String clientIp() {
        return clientIp;
    }

    public String correlationId() {
        return correlationId;
    }

    public boolean isPayloadTruncated() {
        return payloadTruncated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuditLog auditLog)) return false;
        return id.equals(auditLog.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", eventType=" + eventType +
                ", aggregateType='" + aggregateType + '\'' +
                ", aggregateId='" + aggregateId + '\'' +
                ", username='" + username + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", action='" + action + '\'' +
                ", result=" + result +
                ", payloadTruncated=" + payloadTruncated +
                '}';
    }

    /**
     * Builder for creating AuditLog instances.
     */
    public static final class Builder {
        private AuditLogId id;
        private Instant timestamp;
        private AuditEventType eventType;
        private String aggregateType;
        private String aggregateId;
        private String username;
        private String serviceName;
        private String action;
        private String payload;
        private AuditResult result;
        private String errorMessage;
        private String clientIp;
        private String correlationId;
        private boolean payloadTruncated;

        private Builder() {
        }

        public Builder id(AuditLogId id) {
            this.id = id;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder eventType(AuditEventType eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder eventType(String eventType) {
            this.eventType = AuditEventType.of(eventType);
            return this;
        }

        public Builder aggregateType(String aggregateType) {
            this.aggregateType = aggregateType;
            return this;
        }

        public Builder aggregateId(String aggregateId) {
            this.aggregateId = aggregateId;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder payload(String payload) {
            this.payload = payload;
            return this;
        }

        public Builder result(AuditResult result) {
            this.result = result;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder clientIp(String clientIp) {
            this.clientIp = clientIp;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder payloadTruncated(boolean payloadTruncated) {
            this.payloadTruncated = payloadTruncated;
            return this;
        }

        public AuditLog build() {
            return new AuditLog(this);
        }
    }
}
