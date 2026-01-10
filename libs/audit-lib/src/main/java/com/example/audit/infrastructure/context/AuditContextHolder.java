package com.example.audit.infrastructure.context;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Holder for audit context information extracted from various sources.
 *
 * <p>Extracts:</p>
 * <ul>
 *   <li>Username - from SecurityContext (Spring Security)</li>
 *   <li>Client IP - from ServletRequest (with X-Forwarded-For support)</li>
 *   <li>Correlation ID - from MDC (Mapped Diagnostic Context)</li>
 * </ul>
 */
@Component
public class AuditContextHolder {

    private static final String MDC_CORRELATION_ID = "correlationId";
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_X_CORRELATION_ID = "X-Correlation-ID";
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

    /**
     * Gets the current username from SecurityContext.
     *
     * @return the username, or empty if not authenticated
     */
    public Optional<String> getCurrentUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                return Optional.of(auth.getName());
            }
        } catch (Exception e) {
            // Security context not available
        }
        return Optional.empty();
    }

    /**
     * Gets the client IP address from the current request.
     * Supports X-Forwarded-For header for proxied requests.
     *
     * @return the client IP, or empty if not in request context
     */
    public Optional<String> getClientIp() {
        try {
            var requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof ServletRequestAttributes servletAttrs) {
                var request = servletAttrs.getRequest();

                // Check X-Forwarded-For header first (for proxied requests)
                String forwardedFor = request.getHeader(HEADER_X_FORWARDED_FOR);
                if (forwardedFor != null && !forwardedFor.isEmpty()) {
                    // Take the first IP (original client) if multiple
                    return Optional.of(forwardedFor.split(",")[0].trim());
                }

                // Fall back to remote address
                String remoteAddr = request.getRemoteAddr();
                if (remoteAddr != null) {
                    return Optional.of(remoteAddr);
                }
            }
        } catch (Exception e) {
            // Request context not available
        }
        return Optional.empty();
    }

    /**
     * Gets the correlation ID from MDC.
     * Checks multiple common keys: correlationId, traceId, X-Correlation-ID
     *
     * @return the correlation ID, or empty if not set
     */
    public Optional<String> getCorrelationId() {
        // Try different common MDC keys
        String correlationId = MDC.get(MDC_CORRELATION_ID);
        if (correlationId != null && !correlationId.isEmpty()) {
            return Optional.of(correlationId);
        }

        correlationId = MDC.get(MDC_TRACE_ID);
        if (correlationId != null && !correlationId.isEmpty()) {
            return Optional.of(correlationId);
        }

        correlationId = MDC.get(MDC_X_CORRELATION_ID);
        if (correlationId != null && !correlationId.isEmpty()) {
            return Optional.of(correlationId);
        }

        return Optional.empty();
    }
}
