package com.example.audit.infrastructure.event;

import com.example.audit.domain.event.AuditableDomainEvent;
import com.example.audit.domain.port.AuditEventPublisher;
import com.example.audit.infrastructure.config.AuditProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Spring ApplicationEventPublisher adapter for publishing audit events.
 *
 * <p>This adapter bridges the domain port to Spring's event infrastructure,
 * enabling loose coupling between business logic and the audit system.</p>
 */
@Component
public class SpringAuditEventPublisher implements AuditEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SpringAuditEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final AuditProperties properties;

    public SpringAuditEventPublisher(
            ApplicationEventPublisher applicationEventPublisher,
            AuditProperties properties) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.properties = properties;
    }

    @Override
    public void publish(AuditableDomainEvent event) {
        if (!properties.isEnabled()) {
            log.debug("Audit is disabled, skipping event: {}", event.eventType());
            return;
        }

        try {
            log.debug("Publishing audit event: {} for {}/{}",
                    event.eventType(),
                    event.aggregateType(),
                    event.aggregateId());

            applicationEventPublisher.publishEvent(event);

        } catch (Exception e) {
            // FR-005: Audit failure must not affect business operation
            log.error("Failed to publish audit event {}: {}",
                    event.eventType(), e.getMessage(), e);
        }
    }

    @Override
    public boolean publishSync(AuditableDomainEvent event) {
        if (!properties.isEnabled()) {
            log.debug("Audit is disabled, skipping sync event: {}", event.eventType());
            return true;
        }

        try {
            log.debug("Publishing sync audit event: {} for {}/{}",
                    event.eventType(),
                    event.aggregateType(),
                    event.aggregateId());

            // Spring's publishEvent is synchronous by default
            applicationEventPublisher.publishEvent(event);
            return true;

        } catch (Exception e) {
            log.error("Failed to publish sync audit event {}: {}",
                    event.eventType(), e.getMessage(), e);
            return false;
        }
    }
}
