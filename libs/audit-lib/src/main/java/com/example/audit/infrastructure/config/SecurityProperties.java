package com.example.audit.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Security configuration properties for audit-lib.
 *
 * <p>Configuration example:</p>
 * <pre>
 * audit:
 *   security:
 *     enabled: true
 *     issuer-uri: http://localhost:8180/realms/ecommerce
 *     cors:
 *       allowed-origins:
 *         - http://localhost:3000
 *         - http://localhost:8080
 *       allowed-methods:
 *         - GET
 *         - POST
 *         - PUT
 *         - DELETE
 *     public-paths:
 *       - /actuator/health
 *       - /actuator/info
 *     audit-roles:
 *       - ADMIN
 *       - AUDITOR
 *     service-accounts:
 *       - service-name: product-service
 *         client-id: product-service-client
 *       - service-name: user-service
 *         client-id: user-service-client
 * </pre>
 */
@ConfigurationProperties(prefix = "audit.security")
public class SecurityProperties {

    /**
     * Enable or disable security features. Default: true
     */
    private boolean enabled = true;

    /**
     * OAuth2/OIDC issuer URI (e.g., Keycloak realm URL).
     */
    private String issuerUri;

    /**
     * JWK Set URI for JWT validation. If not set, derived from issuerUri.
     */
    private String jwkSetUri;

    /**
     * Expected audience claim in JWT tokens.
     */
    private String audience;

    /**
     * CORS configuration.
     */
    private CorsProperties cors = new CorsProperties();

    /**
     * Paths that don't require authentication.
     */
    private List<String> publicPaths = new ArrayList<>(List.of(
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info"
    ));

    /**
     * Roles that can access audit logs. Default: ADMIN, AUDITOR
     */
    private List<String> auditRoles = new ArrayList<>(List.of("ADMIN", "AUDITOR"));

    /**
     * Service accounts for East-West authentication.
     */
    private List<ServiceAccount> serviceAccounts = new ArrayList<>();

    /**
     * Enable method-level security annotations (@PreAuthorize, @Secured).
     */
    private boolean methodSecurityEnabled = true;

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getJwkSetUri() {
        return jwkSetUri;
    }

    public void setJwkSetUri(String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public CorsProperties getCors() {
        return cors;
    }

    public void setCors(CorsProperties cors) {
        this.cors = cors;
    }

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }

    public List<String> getAuditRoles() {
        return auditRoles;
    }

    public void setAuditRoles(List<String> auditRoles) {
        this.auditRoles = auditRoles;
    }

    public List<ServiceAccount> getServiceAccounts() {
        return serviceAccounts;
    }

    public void setServiceAccounts(List<ServiceAccount> serviceAccounts) {
        this.serviceAccounts = serviceAccounts;
    }

    public boolean isMethodSecurityEnabled() {
        return methodSecurityEnabled;
    }

    public void setMethodSecurityEnabled(boolean methodSecurityEnabled) {
        this.methodSecurityEnabled = methodSecurityEnabled;
    }

    /**
     * CORS configuration properties.
     */
    public static class CorsProperties {

        private boolean enabled = true;

        private List<String> allowedOrigins = new ArrayList<>(List.of("*"));

        private List<String> allowedMethods = new ArrayList<>(List.of(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        private List<String> allowedHeaders = new ArrayList<>(List.of(
                "Authorization", "Content-Type", "X-Correlation-ID", "X-Trace-ID"
        ));

        private List<String> exposedHeaders = new ArrayList<>(List.of(
                "X-Correlation-ID", "X-Trace-ID"
        ));

        private boolean allowCredentials = true;

        private long maxAge = 3600L;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public List<String> getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(List<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public List<String> getExposedHeaders() {
            return exposedHeaders;
        }

        public void setExposedHeaders(List<String> exposedHeaders) {
            this.exposedHeaders = exposedHeaders;
        }

        public boolean isAllowCredentials() {
            return allowCredentials;
        }

        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }

        public long getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(long maxAge) {
            this.maxAge = maxAge;
        }
    }

    /**
     * Service account for East-West authentication.
     */
    public static class ServiceAccount {

        private String serviceName;

        private String clientId;

        private String clientSecret;

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }
    }
}
