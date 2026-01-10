package com.example.audit.domain.port;

import com.example.audit.domain.event.AuditableDomainEvent;

/**
 * Port interface for publishing audit-relevant domain events.
 *
 * <p>This interface follows the Ports and Adapters (Hexagonal) architecture pattern.
 * The domain layer defines this port, and the infrastructure layer provides
 * the implementation (adapter).</p>
 *
 * <p>Usage example:
 * <pre>{@code
 * @Service
 * public class ProductService {
 *     private final AuditEventPublisher eventPublisher;
 *
 *     public Product createProduct(CreateProductCommand cmd) {
 *         Product product = Product.create(cmd);
 *         productRepository.save(product);
 *
 *         eventPublisher.publish(new ProductCreatedEvent(product, currentUser, serviceName));
 *         return product;
 *     }
 * }
 * }</pre>
 * </p>
 */
public interface AuditEventPublisher {

    /**
     * Publishes an auditable domain event.
     *
     * <p>The event will be processed asynchronously by the audit system.
     * Following FR-005 requirement, failures in event processing must not
     * block the business operation.</p>
     *
     * @param event the auditable domain event to publish
     */
    void publish(AuditableDomainEvent event);

    /**
     * Publishes an auditable domain event synchronously.
     *
     * <p>Use this method when you need to ensure the audit log is persisted
     * before continuing. Note that this may impact performance.</p>
     *
     * @param event the auditable domain event to publish
     * @return true if the event was successfully persisted, false otherwise
     */
    default boolean publishSync(AuditableDomainEvent event) {
        publish(event);
        return true;
    }
}
