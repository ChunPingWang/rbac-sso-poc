package com.example.audit.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique identifier for an audit log entry.
 * Immutable and uses value-based equality.
 */
public final class AuditLogId {

    private final UUID value;

    private AuditLogId(UUID value) {
        this.value = Objects.requireNonNull(value, "AuditLogId value must not be null");
    }

    /**
     * Creates an AuditLogId from an existing UUID.
     *
     * @param value the UUID value
     * @return a new AuditLogId
     */
    public static AuditLogId of(UUID value) {
        return new AuditLogId(value);
    }

    /**
     * Creates an AuditLogId from a string representation of a UUID.
     *
     * @param value the string UUID
     * @return a new AuditLogId
     */
    public static AuditLogId of(String value) {
        return new AuditLogId(UUID.fromString(value));
    }

    /**
     * Generates a new random AuditLogId.
     *
     * @return a new randomly generated AuditLogId
     */
    public static AuditLogId generate() {
        return new AuditLogId(UUID.randomUUID());
    }

    /**
     * Returns the UUID value.
     *
     * @return the UUID value
     */
    public UUID value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuditLogId that)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
