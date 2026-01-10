package com.example.audit.infrastructure.event;

import com.example.audit.domain.event.AuditableDomainEvent;
import com.example.audit.domain.model.AuditEventType;
import com.example.audit.domain.model.AuditLog;
import com.example.audit.domain.model.AuditLogId;
import com.example.audit.domain.model.AuditResult;
import com.example.audit.domain.port.AuditLogRepository;
import com.example.audit.infrastructure.metrics.AuditMetrics;
import com.example.audit.infrastructure.processor.PayloadProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Event listener that processes auditable domain events and persists them to the audit log.
 *
 * <p>This listener captures all {@link AuditableDomainEvent} instances published through
 * Spring's event system and transforms them into {@link AuditLog} entries.</p>
 *
 * <p>The listener runs in a separate transaction (REQUIRES_NEW) to ensure audit log
 * persistence is independent of the business transaction, following FR-005 requirement.</p>
 */
@Component
public class AuditDomainEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditDomainEventListener.class);

    private final AuditLogRepository repository;
    private final PayloadProcessor payloadProcessor;
    private final AuditMetrics metrics;

    public AuditDomainEventListener(
            AuditLogRepository repository,
            PayloadProcessor payloadProcessor,
            AuditMetrics metrics) {
        this.repository = repository;
        this.payloadProcessor = payloadProcessor;
        this.metrics = metrics;
    }

    /**
     * Handles auditable domain events and persists them to the audit log.
     *
     * <p>This method runs asynchronously (@Async) to avoid blocking the main thread,
     * and uses a new transaction (REQUIRES_NEW) to ensure audit persistence is
     * independent of the business transaction.</p>
     *
     * @param event the auditable domain event to process
     */
    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAuditEvent(AuditableDomainEvent event) {
        long startTime = System.currentTimeMillis();

        try {
            log.debug("Processing audit event: {} for {}/{}",
                    event.eventType(),
                    event.aggregateType(),
                    event.aggregateId());

            // Process payload with masking if configured
            String processedPayload = processPayload(event);

            // Build audit log entry
            AuditLog auditLog = AuditLog.builder()
                    .id(AuditLogId.of(event.eventId()))
                    .timestamp(event.occurredAt())
                    .eventType(AuditEventType.of(event.eventType()))
                    .aggregateType(event.aggregateType())
                    .aggregateId(event.aggregateId())
                    .username(event.username())
                    .serviceName(event.serviceName())
                    .action(event.action())
                    .payload(processedPayload)
                    .result(event.isSuccess() ? AuditResult.SUCCESS : AuditResult.FAILURE)
                    .errorMessage(event.errorMessage())
                    .clientIp(event.clientIp())
                    .correlationId(event.correlationId())
                    .payloadTruncated(event.isPayloadTruncated())
                    .build();

            // Persist to repository
            repository.save(auditLog);

            // Record metrics
            long latency = System.currentTimeMillis() - startTime;
            metrics.recordLatency(latency);
            metrics.incrementTotal();

            log.debug("Successfully persisted audit log: {} in {}ms",
                    event.eventType(), latency);

        } catch (Exception e) {
            // FR-005: Log error but don't propagate - audit failure must not affect business
            log.error("Failed to persist audit event {}: {}",
                    event.eventType(), e.getMessage(), e);
            metrics.incrementFailed();
        }
    }

    /**
     * Handles auditable domain events synchronously.
     *
     * <p>Use this method when audit persistence must complete before returning.
     * This is useful for critical audit events that must be captured immediately.</p>
     *
     * @param event the auditable domain event to process
     * @return true if successfully persisted, false otherwise
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean handleAuditEventSync(AuditableDomainEvent event) {
        long startTime = System.currentTimeMillis();

        try {
            log.debug("Processing sync audit event: {} for {}/{}",
                    event.eventType(),
                    event.aggregateType(),
                    event.aggregateId());

            String processedPayload = processPayload(event);

            AuditLog auditLog = AuditLog.builder()
                    .id(AuditLogId.of(event.eventId()))
                    .timestamp(event.occurredAt())
                    .eventType(AuditEventType.of(event.eventType()))
                    .aggregateType(event.aggregateType())
                    .aggregateId(event.aggregateId())
                    .username(event.username())
                    .serviceName(event.serviceName())
                    .action(event.action())
                    .payload(processedPayload)
                    .result(event.isSuccess() ? AuditResult.SUCCESS : AuditResult.FAILURE)
                    .errorMessage(event.errorMessage())
                    .clientIp(event.clientIp())
                    .correlationId(event.correlationId())
                    .payloadTruncated(event.isPayloadTruncated())
                    .build();

            repository.save(auditLog);

            long latency = System.currentTimeMillis() - startTime;
            metrics.recordLatency(latency);
            metrics.incrementTotal();

            log.debug("Successfully persisted sync audit log: {} in {}ms",
                    event.eventType(), latency);

            return true;

        } catch (Exception e) {
            log.error("Failed to persist sync audit event {}: {}",
                    event.eventType(), e.getMessage(), e);
            metrics.incrementFailed();
            return false;
        }
    }

    private String processPayload(AuditableDomainEvent event) {
        if (event.payload() == null) {
            return null;
        }

        String[] maskFields = event.maskFields();
        if (maskFields == null || maskFields.length == 0) {
            return event.payload();
        }

        // Use payload processor to mask sensitive fields
        PayloadProcessor.ProcessedPayload processed = payloadProcessor.processJsonPayload(
                event.payload(), maskFields);

        return processed.payload();
    }
}
