package com.example.audit.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Abstract base class providing common implementation for auditable domain events.
 *
 * <p>Extend this class to create specific audit events for your domain operations.
 * This reduces boilerplate while ensuring all required audit information is captured.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * public class ProductCreatedEvent extends BaseAuditEvent {
 *     private final Product product;
 *
 *     public ProductCreatedEvent(Product product, String username, String serviceName) {
 *         super("PRODUCT_CREATED", "Product", product.getId().toString(),
 *               "createProduct", username, serviceName, toJson(product), true, null);
 *         this.product = product;
 *     }
 *
 *     public Product getProduct() {
 *         return product;
 *     }
 * }
 * }</pre>
 * </p>
 */
public abstract class BaseAuditEvent implements AuditableDomainEvent {

    private final UUID eventId;
    private final Instant occurredAt;
    private final String eventType;
    private final String aggregateType;
    private final String aggregateId;
    private final String action;
    private final String username;
    private final String serviceName;
    private final String payload;
    private final boolean success;
    private final String errorMessage;
    private final String clientIp;
    private final String correlationId;
    private final boolean payloadTruncated;
    private final String[] maskFields;

    /**
     * Creates a new base audit event with all required fields.
     *
     * @param eventType     the event type name (UPPER_SNAKE_CASE)
     * @param aggregateType the type of aggregate
     * @param aggregateId   the aggregate identifier
     * @param action        the business action
     * @param username      the username of the actor
     * @param serviceName   the service name
     * @param payload       the event payload
     * @param success       whether the operation succeeded
     * @param errorMessage  error message if operation failed
     */
    protected BaseAuditEvent(
            String eventType,
            String aggregateType,
            String aggregateId,
            String action,
            String username,
            String serviceName,
            String payload,
            boolean success,
            String errorMessage) {
        this(eventType, aggregateType, aggregateId, action, username, serviceName,
                payload, success, errorMessage, null, null, false, new String[0]);
    }

    /**
     * Creates a new base audit event with all fields including optional ones.
     */
    protected BaseAuditEvent(
            String eventType,
            String aggregateType,
            String aggregateId,
            String action,
            String username,
            String serviceName,
            String payload,
            boolean success,
            String errorMessage,
            String clientIp,
            String correlationId,
            boolean payloadTruncated,
            String[] maskFields) {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.aggregateType = Objects.requireNonNull(aggregateType, "aggregateType must not be null");
        this.aggregateId = aggregateId;
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.username = Objects.requireNonNull(username, "username must not be null");
        this.serviceName = Objects.requireNonNull(serviceName, "serviceName must not be null");
        this.payload = payload;
        this.success = success;
        this.errorMessage = errorMessage;
        this.clientIp = clientIp;
        this.correlationId = correlationId;
        this.payloadTruncated = payloadTruncated;
        this.maskFields = maskFields != null ? maskFields.clone() : new String[0];
    }

    @Override
    public UUID eventId() {
        return eventId;
    }

    @Override
    public Instant occurredAt() {
        return occurredAt;
    }

    @Override
    public String eventType() {
        return eventType;
    }

    @Override
    public String aggregateType() {
        return aggregateType;
    }

    @Override
    public String aggregateId() {
        return aggregateId;
    }

    @Override
    public String action() {
        return action;
    }

    @Override
    public String username() {
        return username;
    }

    @Override
    public String serviceName() {
        return serviceName;
    }

    @Override
    public String payload() {
        return payload;
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    @Override
    public String errorMessage() {
        return errorMessage;
    }

    @Override
    public String clientIp() {
        return clientIp;
    }

    @Override
    public String correlationId() {
        return correlationId;
    }

    @Override
    public boolean isPayloadTruncated() {
        return payloadTruncated;
    }

    @Override
    public String[] maskFields() {
        return maskFields.clone();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "eventId=" + eventId +
                ", eventType='" + eventType + '\'' +
                ", aggregateType='" + aggregateType + '\'' +
                ", aggregateId='" + aggregateId + '\'' +
                ", action='" + action + '\'' +
                ", success=" + success +
                '}';
    }
}
