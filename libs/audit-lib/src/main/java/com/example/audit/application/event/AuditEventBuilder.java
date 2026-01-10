package com.example.audit.application.event;

import com.example.audit.domain.event.AuditableDomainEvent;
import com.example.audit.domain.event.BaseAuditEvent;
import com.example.audit.infrastructure.config.AuditProperties;
import com.example.audit.infrastructure.context.AuditContextHolder;
import com.example.audit.infrastructure.processor.PayloadProcessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;

/**
 * Builder component for creating auditable domain events with automatic context enrichment.
 *
 * <p>This builder simplifies the creation of audit events by automatically:
 * <ul>
 *   <li>Extracting username from security context</li>
 *   <li>Getting client IP from request context</li>
 *   <li>Setting correlation ID from MDC</li>
 *   <li>Using configured service name</li>
 *   <li>Serializing payloads to JSON</li>
 * </ul>
 * </p>
 *
 * <p>Example usage:
 * <pre>{@code
 * @Service
 * public class ProductService {
 *     private final AuditEventBuilder auditEventBuilder;
 *     private final AuditEventPublisher eventPublisher;
 *
 *     public Product createProduct(CreateProductCommand cmd) {
 *         Product product = Product.create(cmd);
 *         productRepository.save(product);
 *
 *         eventPublisher.publish(auditEventBuilder.success()
 *             .eventType("PRODUCT_CREATED")
 *             .aggregateType("Product")
 *             .aggregateId(product.getId().toString())
 *             .action("createProduct")
 *             .payload(product)
 *             .build());
 *
 *         return product;
 *     }
 * }
 * }</pre>
 * </p>
 */
@Component
public class AuditEventBuilder {

    private static final Logger log = LoggerFactory.getLogger(AuditEventBuilder.class);
    private static final String DEFAULT_USERNAME = "ANONYMOUS";
    private static final String DEFAULT_CLIENT_IP = "unknown";

    private final AuditContextHolder contextHolder;
    private final AuditProperties properties;
    private final ObjectMapper objectMapper;

    public AuditEventBuilder(
            AuditContextHolder contextHolder,
            AuditProperties properties,
            ObjectMapper objectMapper) {
        this.contextHolder = contextHolder;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a builder for a successful operation.
     *
     * @return a new event builder instance
     */
    public Builder success() {
        return new Builder(true, null);
    }

    /**
     * Creates a builder for a failed operation.
     *
     * @param errorMessage the error message
     * @return a new event builder instance
     */
    public Builder failure(String errorMessage) {
        return new Builder(false, errorMessage);
    }

    /**
     * Creates a builder for a failed operation with exception.
     *
     * @param exception the exception that caused the failure
     * @return a new event builder instance
     */
    public Builder failure(Throwable exception) {
        return new Builder(false, exception.getMessage());
    }

    /**
     * Inner builder class for constructing audit events.
     */
    public class Builder {
        private final boolean success;
        private final String errorMessage;
        private String eventType;
        private String aggregateType;
        private String aggregateId;
        private String action;
        private String payload;
        private boolean payloadTruncated;
        private String[] maskFields;
        private String usernameOverride;
        private String clientIpOverride;
        private String correlationIdOverride;

        private Builder(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.maskFields = new String[0];
        }

        public Builder eventType(String eventType) {
            this.eventType = eventType;
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

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        /**
         * Sets the payload as a raw JSON string.
         */
        public Builder payload(String jsonPayload) {
            this.payload = jsonPayload;
            return this;
        }

        /**
         * Sets the payload by serializing the object to JSON.
         */
        public Builder payload(Object object) {
            if (object == null) {
                this.payload = null;
                return this;
            }
            try {
                String json = objectMapper.writeValueAsString(object);
                int maxSize = properties.getPayload().getMaxSize();
                if (json.length() > maxSize) {
                    this.payload = json.substring(0, maxSize);
                    this.payloadTruncated = true;
                } else {
                    this.payload = json;
                }
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize payload", e);
                this.payload = "{\"_error\":\"serialization failed\"}";
            }
            return this;
        }

        public Builder maskFields(String... fields) {
            this.maskFields = fields != null ? fields : new String[0];
            return this;
        }

        public Builder username(String username) {
            this.usernameOverride = username;
            return this;
        }

        public Builder clientIp(String clientIp) {
            this.clientIpOverride = clientIp;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationIdOverride = correlationId;
            return this;
        }

        /**
         * Builds the auditable domain event with all context information.
         *
         * @return the constructed event
         */
        public AuditableDomainEvent build() {
            Objects.requireNonNull(eventType, "eventType must not be null");
            Objects.requireNonNull(aggregateType, "aggregateType must not be null");
            Objects.requireNonNull(action, "action must not be null");

            String username = usernameOverride != null
                    ? usernameOverride
                    : contextHolder.getCurrentUsername().orElse(DEFAULT_USERNAME);

            String clientIp = clientIpOverride != null
                    ? clientIpOverride
                    : contextHolder.getClientIp().orElse(DEFAULT_CLIENT_IP);

            String correlationId = correlationIdOverride != null
                    ? correlationIdOverride
                    : contextHolder.getCorrelationId().orElse(null);

            String serviceName = properties.getServiceName() != null
                    ? properties.getServiceName()
                    : "unknown-service";

            return new BaseAuditEvent(
                    eventType,
                    aggregateType,
                    aggregateId,
                    action,
                    username,
                    serviceName,
                    payload,
                    success,
                    errorMessage,
                    clientIp,
                    correlationId,
                    payloadTruncated,
                    maskFields
            ) {};
        }
    }
}
