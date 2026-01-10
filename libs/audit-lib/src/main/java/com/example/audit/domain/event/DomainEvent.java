package com.example.audit.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all domain events in the system.
 * Domain events represent something meaningful that happened in the business domain.
 *
 * <p>Following DDD principles, domain events are immutable and capture facts
 * that have occurred in the past. They enable loose coupling between aggregates
 * and services.</p>
 */
public interface DomainEvent {

    /**
     * Returns the unique identifier for this event instance.
     *
     * @return the event ID
     */
    UUID eventId();

    /**
     * Returns the timestamp when this event occurred.
     *
     * @return the event timestamp
     */
    Instant occurredAt();

    /**
     * Returns the type name of this event.
     * Should follow UPPER_SNAKE_CASE convention (e.g., PRODUCT_CREATED).
     *
     * @return the event type name
     */
    String eventType();
}
