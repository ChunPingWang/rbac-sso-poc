package com.example.audit.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object representing the type of an audit event.
 * Should follow UPPER_SNAKE_CASE convention (e.g., PRODUCT_CREATED, USER_PROFILE_UPDATED).
 * Immutable and uses value-based equality.
 */
public final class AuditEventType {

    private static final Pattern UPPER_SNAKE_CASE = Pattern.compile("^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$");

    private final String value;

    private AuditEventType(String value) {
        Objects.requireNonNull(value, "AuditEventType value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("AuditEventType value must not be blank");
        }
        this.value = value;
    }

    /**
     * Creates an AuditEventType from a string value.
     * The value should follow UPPER_SNAKE_CASE convention.
     *
     * @param value the event type name
     * @return a new AuditEventType
     * @throws IllegalArgumentException if the value is null or blank
     */
    public static AuditEventType of(String value) {
        return new AuditEventType(value);
    }

    /**
     * Returns the event type value.
     *
     * @return the event type value
     */
    public String value() {
        return value;
    }

    /**
     * Checks if the event type follows UPPER_SNAKE_CASE convention.
     *
     * @return true if the value follows UPPER_SNAKE_CASE convention
     */
    public boolean isValidFormat() {
        return UPPER_SNAKE_CASE.matcher(value).matches();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuditEventType that)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
