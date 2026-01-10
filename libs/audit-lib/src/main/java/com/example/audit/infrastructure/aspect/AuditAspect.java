package com.example.audit.infrastructure.aspect;

import com.example.audit.annotation.Auditable;
import com.example.audit.domain.model.AuditEventType;
import com.example.audit.domain.model.AuditLog;
import com.example.audit.domain.model.AuditLogId;
import com.example.audit.domain.model.AuditResult;
import com.example.audit.domain.port.AuditLogRepository;
import com.example.audit.infrastructure.config.AuditProperties;
import com.example.audit.infrastructure.context.AuditContextHolder;
import com.example.audit.infrastructure.metrics.AuditMetrics;
import com.example.audit.infrastructure.processor.PayloadProcessor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * AOP aspect for automatic audit logging.
 *
 * <p>Intercepts methods annotated with @Auditable and captures audit information
 * before/after method execution. Implements FR-005: audit failures MUST NOT
 * block business operations.</p>
 *
 * <p>Execution order is set to lowest precedence minus 10 to ensure audit
 * capture happens after transaction commit.</p>
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
    private static final String DEFAULT_USERNAME = "ANONYMOUS";
    private static final String DEFAULT_CLIENT_IP = "unknown";

    private final AuditLogRepository repository;
    private final PayloadProcessor payloadProcessor;
    private final AuditContextHolder contextHolder;
    private final AuditMetrics metrics;
    private final AuditProperties properties;

    public AuditAspect(
            AuditLogRepository repository,
            PayloadProcessor payloadProcessor,
            AuditContextHolder contextHolder,
            AuditMetrics metrics,
            AuditProperties properties) {
        this.repository = repository;
        this.payloadProcessor = payloadProcessor;
        this.contextHolder = contextHolder;
        this.metrics = metrics;
        this.properties = properties;
    }

    /**
     * Intercepts methods annotated with @Auditable and captures audit information.
     *
     * @param joinPoint the join point representing the method invocation
     * @param auditable the @Auditable annotation
     * @return the result of the method invocation
     * @throws Throwable if the target method throws an exception
     */
    @Around("@annotation(auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }

        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;

        try {
            // Execute the business method
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            // FR-005: Audit capture in finally block - failures must not block business
            captureAuditLog(joinPoint, auditable, result, error, startTime);
        }
    }

    private void captureAuditLog(
            ProceedingJoinPoint joinPoint,
            Auditable auditable,
            Object result,
            Throwable error,
            long startTime) {
        try {
            // Extract context information
            String username = contextHolder.getCurrentUsername().orElse(DEFAULT_USERNAME);
            String clientIp = contextHolder.getClientIp().orElse(DEFAULT_CLIENT_IP);
            String correlationId = contextHolder.getCorrelationId().orElse(null);
            String serviceName = properties.getServiceName();

            // Process payload
            PayloadProcessor.ProcessedPayload processedPayload = payloadProcessor.process(
                    joinPoint.getArgs(),
                    auditable.maskFields()
            );

            // Extract aggregate ID (simplified - could use SpEL in full implementation)
            String aggregateId = extractAggregateId(joinPoint, result, auditable);

            // Build audit log
            AuditLog auditLog = AuditLog.builder()
                    .id(AuditLogId.generate())
                    .timestamp(Instant.now())
                    .eventType(AuditEventType.of(auditable.eventType()))
                    .aggregateType(auditable.resourceType())
                    .aggregateId(aggregateId)
                    .username(username)
                    .serviceName(serviceName != null ? serviceName : "unknown-service")
                    .action(joinPoint.getSignature().getName())
                    .payload(processedPayload.payload())
                    .result(error == null ? AuditResult.SUCCESS : AuditResult.FAILURE)
                    .errorMessage(error != null ? error.getMessage() : null)
                    .clientIp(clientIp)
                    .correlationId(correlationId)
                    .payloadTruncated(processedPayload.isTruncated())
                    .build();

            // Save to repository
            repository.save(auditLog);

            // Record metrics
            long latency = System.currentTimeMillis() - startTime;
            metrics.recordLatency(latency);
            metrics.incrementTotal();

            log.debug("Captured audit log: {} for {}", auditable.eventType(), auditable.resourceType());

        } catch (Exception e) {
            // FR-005: Audit failure must not affect business operation
            log.error("Failed to capture audit log for {}: {}", auditable.eventType(), e.getMessage(), e);
            metrics.incrementFailed();
        }
    }

    private String extractAggregateId(ProceedingJoinPoint joinPoint, Object result, Auditable auditable) {
        // If SpEL expression is provided, could evaluate it here
        // For now, try simple extraction from result

        if (result != null) {
            // Try common ID extraction patterns
            try {
                // Check if result has an id() method (like our value objects)
                var idMethod = result.getClass().getMethod("id");
                Object id = idMethod.invoke(result);
                if (id != null) {
                    return id.toString();
                }
            } catch (NoSuchMethodException e) {
                // Try getId()
                try {
                    var getIdMethod = result.getClass().getMethod("getId");
                    Object id = getIdMethod.invoke(result);
                    if (id != null) {
                        return id.toString();
                    }
                } catch (Exception ignored) {
                }
            } catch (Exception e) {
                // Ignore extraction failures
            }

            // If result is a simple type (UUID, String, Number), use it directly
            if (result instanceof java.util.UUID || result instanceof String || result instanceof Number) {
                return result.toString();
            }
        }

        // Try to extract from first argument if it has an ID
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0 && args[0] != null) {
            try {
                var idMethod = args[0].getClass().getMethod("id");
                Object id = idMethod.invoke(args[0]);
                if (id != null) {
                    return id.toString();
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }
}
