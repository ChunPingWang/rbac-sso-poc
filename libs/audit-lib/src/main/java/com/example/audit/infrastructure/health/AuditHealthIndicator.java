package com.example.audit.infrastructure.health;

import com.example.audit.domain.model.AuditLogId;
import com.example.audit.domain.port.AuditLogRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Health indicator for the audit system.
 *
 * <p>Reports health based on ability to connect to audit storage.
 * Exposed at /actuator/health/audit</p>
 */
@Component
public class AuditHealthIndicator implements HealthIndicator {

    private final AuditLogRepository repository;

    public AuditHealthIndicator(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public Health health() {
        try {
            // Simple connectivity check - try to query with a non-existent ID
            repository.existsById(AuditLogId.of(UUID.fromString("00000000-0000-0000-0000-000000000000")));
            return Health.up()
                    .withDetail("storage", "connected")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("storage", "disconnected")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
