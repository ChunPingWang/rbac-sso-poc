package com.example.audit.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for the audit library.
 *
 * <p>Properties are bound from application configuration using the "audit" prefix:</p>
 * <pre>{@code
 * audit:
 *   enabled: true
 *   service-name: product-service
 *   payload:
 *     max-size: 65536
 *   masking:
 *     default-fields:
 *       - password
 *       - secret
 * }</pre>
 *
 * <p>To enable dynamic configuration reload, use with @RefreshScope:</p>
 * <pre>{@code
 * @RefreshScope
 * @ConfigurationProperties(prefix = "audit")
 * public class AuditProperties { ... }
 * }</pre>
 */
@ConfigurationProperties(prefix = "audit")
public class AuditProperties {

    /**
     * Whether audit logging is enabled. Default: true
     */
    private boolean enabled = true;

    /**
     * The service name used to identify the originating microservice in audit logs.
     * If not set, attempts to use spring.application.name.
     */
    private String serviceName;

    /**
     * Payload configuration.
     */
    private Payload payload = new Payload();

    /**
     * Masking configuration.
     */
    private Masking masking = new Masking();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    public Masking getMasking() {
        return masking;
    }

    public void setMasking(Masking masking) {
        this.masking = masking;
    }

    /**
     * Payload-related configuration.
     */
    public static class Payload {

        /**
         * Maximum payload size in bytes. Default: 65536 (64 KB)
         * Payloads exceeding this limit will be truncated.
         */
        private int maxSize = 65536;

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }
    }

    /**
     * Field masking configuration.
     */
    public static class Masking {

        /**
         * Default fields to mask across all audited operations.
         * These are applied in addition to fields specified in @Auditable.maskFields.
         */
        private List<String> defaultFields = new ArrayList<>(List.of(
                "password",
                "secret",
                "token",
                "credential",
                "apiKey",
                "accessToken",
                "refreshToken"
        ));

        public List<String> getDefaultFields() {
            return defaultFields;
        }

        public void setDefaultFields(List<String> defaultFields) {
            this.defaultFields = defaultFields;
        }
    }
}
