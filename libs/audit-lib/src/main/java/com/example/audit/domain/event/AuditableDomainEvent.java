package com.example.audit.domain.event;

/**
 * Interface for domain events that should be captured in the audit trail.
 *
 * <p>Implementing this interface signals that the event contains audit-relevant
 * information and should be persisted to the audit log. This provides a clean
 * alternative to AOP-based auditing, allowing domain logic to explicitly publish
 * audit events.</p>
 *
 * <p>Benefits over AOP approach:
 * <ul>
 *   <li>Explicit control over what gets audited</li>
 *   <li>Domain events carry rich contextual information</li>
 *   <li>Better testability - events can be verified directly</li>
 *   <li>Decoupled from method signatures</li>
 *   <li>Supports complex business workflows with multiple audit points</li>
 * </ul>
 * </p>
 */
public interface AuditableDomainEvent extends DomainEvent {

    /**
     * Returns the type of aggregate this event relates to.
     * Examples: "Product", "Order", "User"
     *
     * @return the aggregate type
     */
    String aggregateType();

    /**
     * Returns the unique identifier of the aggregate instance.
     *
     * @return the aggregate ID, may be null for events without specific aggregate
     */
    String aggregateId();

    /**
     * Returns the business action that triggered this event.
     * Examples: "createProduct", "placeOrder", "updateProfile"
     *
     * @return the action name
     */
    String action();

    /**
     * Returns the username of the actor who triggered this event.
     *
     * @return the username
     */
    String username();

    /**
     * Returns the name of the service where this event originated.
     *
     * @return the service name
     */
    String serviceName();

    /**
     * Returns the event payload as a serialized string.
     * This should contain relevant business data for audit purposes.
     * Sensitive fields should already be masked.
     *
     * @return the payload string
     */
    String payload();

    /**
     * Returns whether the operation was successful.
     *
     * @return true if successful, false otherwise
     */
    boolean isSuccess();

    /**
     * Returns the error message if the operation failed.
     *
     * @return the error message, or null if operation was successful
     */
    String errorMessage();

    /**
     * Returns the client IP address of the actor.
     *
     * @return the client IP address, may be null
     */
    String clientIp();

    /**
     * Returns the correlation ID for distributed tracing.
     *
     * @return the correlation ID, may be null
     */
    String correlationId();

    /**
     * Returns whether the payload was truncated due to size limits.
     *
     * @return true if payload was truncated
     */
    default boolean isPayloadTruncated() {
        return false;
    }

    /**
     * Returns fields to mask in the audit log.
     *
     * @return array of field names to mask
     */
    default String[] maskFields() {
        return new String[0];
    }
}
